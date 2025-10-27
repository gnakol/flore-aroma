package fr.ecommerce.flore.aroma.flore_aroma.whatsapp.service;

import fr.ecommerce.flore.aroma.flore_aroma.whatsapp.bean.PhoneVerification;
import fr.ecommerce.flore.aroma.flore_aroma.whatsapp.repositories.PhoneVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WhatsAppService {
    @Value("${whatsapp.phone-number-id}")
    private String phoneNumberId;
    @Value("${whatsapp.token}")
    private String waToken; // Bearer
    @Value("${app.public-base-url}")
    private String publicBaseUrl; // https://ton-domaine

    private final PhoneVerificationRepository phoneVerificationRepository;

    private final RestTemplate rest = new RestTemplate();

    public void sendVerificationLink(String phoneE164, String code) {
        String verifyUrl = publicBaseUrl + "/flore-api/account/verify-phone?code=" + code;

        String recipient = phoneE164.replace("+", "");
        String url = "https://graph.facebook.com/v20.0/" + phoneNumberId + "/messages";

        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(waToken);
        h.setContentType(MediaType.APPLICATION_JSON);

        // Message texte simple au lieu de template
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", recipient,
                "type", "text",
                "text", Map.of(
                        "body", "🌺 Flore Parfums - Cliquez pour valider votre compte : " + verifyUrl
                )
        );

        rest.postForEntity(url, new HttpEntity<>(payload, h), Map.class);
    }

    public void remove(Long id)
    {
        Optional<PhoneVerification> phoneVerification = this.phoneVerificationRepository.findById(id);
        if (phoneVerification.isEmpty())
            throw new RuntimeException("Phone verification with ID : " +id+ " was not found");
        this.phoneVerificationRepository.delete(phoneVerification.get());
    }

}

