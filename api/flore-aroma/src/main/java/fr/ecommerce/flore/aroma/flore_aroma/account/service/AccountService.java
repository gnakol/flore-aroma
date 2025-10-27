package fr.ecommerce.flore.aroma.flore_aroma.account.service;


import fr.ecommerce.flore.aroma.flore_aroma.account.bean.Account;
import fr.ecommerce.flore.aroma.flore_aroma.account.dto.RegisterRequest;
import fr.ecommerce.flore.aroma.flore_aroma.account.dto.RegisterResponse;
import fr.ecommerce.flore.aroma.flore_aroma.account.repositories.AccountRepository;
import fr.ecommerce.flore.aroma.flore_aroma.keycloak.service.KeycloakAdminService;
import fr.ecommerce.flore.aroma.flore_aroma.whatsapp.bean.PhoneVerification;
import fr.ecommerce.flore.aroma.flore_aroma.whatsapp.repositories.PhoneVerificationRepository;
import fr.ecommerce.flore.aroma.flore_aroma.whatsapp.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final PhoneVerificationRepository phoneRepo;
    private final KeycloakAdminService keycloakAdminService;
    private final WhatsAppService whatsapp;

    private static final Duration VERIF_TTL = Duration.ofMinutes(20);

    private boolean isAfrican(String iso2) {
        if (iso2 == null) return false;
        String x = iso2.trim().toUpperCase();
        return switch (x) {
            case "CI","GN","SN","ML","BF","TG","BJ","GA","CM","CD","CG","TD",
                 "NE","NG","GH","ZA","MA","TN","DZ","KE","UG","TZ","ET","RW","BI",
                 "GW","SL","LR","GM","MR","CF","AO","ZW","ZM","MW","MZ","NA","BW","LS","SZ" -> true;
            default -> false;
        };
    }

    @Transactional
    public RegisterResponse registerNonAfricaEmailFlow(RegisterRequest req) {
        if (isAfrican(req.getCountryCode())) {
            throw new IllegalArgumentException("Ce flow est réservé au cas non-Afrique (email-first).");
        }
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email requis pour le flow non-Afrique.");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new IllegalArgumentException("Mot de passe requis.");
        }

        // 1) Créer l’utilisateur Keycloak (VERIFY_EMAIL only)
        String kcUserId = keycloakAdminService.createUserWithEmailRequired(
                req.getEmail(),
                req.getFirstName(),
                req.getLastName()
        );

        // 2) Définir le mot de passe dans Keycloak (ne rien stocker localement)
        keycloakAdminService.setUserPassword(kcUserId, req.getPassword(), false);

        keycloakAdminService.addRealmRoleToUser(kcUserId, "CUSTOMER");

        // 3) Persister local (is_active=false tant que l'email n'est pas vérifié)
        Account acc = Account.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .phoneE164(req.getPhoneE164())
                .countryCode(req.getCountryCode())
                .accountType("CUSTOMER")
                .active(true)
                .keycloakUserId(kcUserId)
                .build();

        acc = accountRepository.save(acc);

        // 4) Envoyer l’email de vérification via Keycloak
        keycloakAdminService.sendVerifyEmail(kcUserId);

        return new RegisterResponse(acc.getId(), kcUserId, true);
    }

    private String technicalEmail(String phoneE164, String iso2) {
        String local = phoneE164.replace("+","");
        String tld = iso2 == null ? "af" : iso2.trim().toLowerCase();
        return local + "@florearoma." + tld;
    }

    @Transactional
    public RegisterResponse registerAfricaPhoneFlow(RegisterRequest req) {
        if (req.getPhoneE164() == null || req.getPhoneE164().isBlank())
            throw new IllegalArgumentException("Téléphone requis");
        if (req.getPassword() == null || req.getPassword().isBlank())
            throw new IllegalArgumentException("Mot de passe requis");

        String email = (req.getEmail() == null || req.getEmail().isBlank())
                ? technicalEmail(req.getPhoneE164(), req.getCountryCode())
                : req.getEmail();

        // 1) Keycloak user
        String kcUserId = keycloakAdminService.createUserWithoutRequiredActions(
                email, req.getFirstName(), req.getLastName());
        keycloakAdminService.setUserPassword(kcUserId, req.getPassword(), false);
        keycloakAdminService.addRealmRoleToUser(kcUserId, "CUSTOMER");
        keycloakAdminService.enableUser(kcUserId, true);            // activé, mais emailVerified=false (par défaut)

        // 2) Local account
        Account acc = Account.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(email)
                .phoneE164(req.getPhoneE164())
                .countryCode(req.getCountryCode())
                .accountType("CUSTOMER")
                .active(false)                        // on met false tant que le téléphone n’est pas validé
                .keycloakUserId(kcUserId)
                .build();
        acc = accountRepository.save(acc);

        // 3) Générer code & envoyer WhatsApp
        String code = UUID.randomUUID().toString().replace("-", "");
        phoneRepo.save(PhoneVerification.builder()
                .keycloakUserId(kcUserId)
                .phoneE164(req.getPhoneE164())
                .code(code)
                .expiresAt(Instant.now().plus(VERIF_TTL))
                .used(false)
                .build());

        whatsapp.sendVerificationLink(req.getPhoneE164(), code);

        return new RegisterResponse(acc.getId(), kcUserId, true);
    }

    @Transactional
    public void confirmPhone(String code) {
        PhoneVerification pv = phoneRepo.findByCodeAndUsedFalse(code)
                .orElseThrow(() -> new RuntimeException("Code invalide"));
        if (pv.getExpiresAt().isBefore(Instant.now()))
            throw new RuntimeException("Code expiré");

        // 1) marquer l’email comme vérifié
        keycloakAdminService.markEmailVerified(pv.getKeycloakUserId(), true);

        // 2) vider toute required action résiduelle (sécurité)
        keycloakAdminService.clearRequiredActions(pv.getKeycloakUserId());

        // 3) s’assurer que le user est enabled
        keycloakAdminService.enableUser(pv.getKeycloakUserId(), true);

        // 4) activer côté DB
        accountRepository.findByKeycloakUserId(pv.getKeycloakUserId())
                .ifPresent(a -> { a.setActive(true); accountRepository.save(a); });

        pv.setUsed(true);
        phoneRepo.save(pv);
    }


    @Transactional
    public void removeAccount(Long idAccount) {
        Account acc = accountRepository.findById(idAccount)
                .orElseThrow(() -> new RuntimeException("Account with ID : " + idAccount + " was not found"));

        // 1) tenter la suppression côté Keycloak (idempotent : on continue même si 404)
        String kcUserId = acc.getKeycloakUserId();
        if (kcUserId != null && !kcUserId.isBlank()) {
            try {
                keycloakAdminService.deleteUser(kcUserId);
            } catch (org.springframework.web.client.HttpClientErrorException.NotFound nf) {
                // déjà supprimé côté KC → on ignore
            } catch (Exception e) {
                // selon ta politique : soit on stoppe, soit on log et on continue
                // Ici: on remonte une erreur claire
                throw new RuntimeException("Keycloak deletion failed for userId=" + kcUserId + " : " + e.getMessage(), e);
            }
        }

        // 2) suppression locale
        accountRepository.delete(acc);
    }





    public void removeLocal(Long idAccount)
    {
        Optional<Account> account = this.accountRepository.findById(idAccount);

        if (account.isEmpty())
            throw new RuntimeException("Account with ID : "+idAccount+ "was not fount");

        this.accountRepository.delete(account.get());
    }
}
