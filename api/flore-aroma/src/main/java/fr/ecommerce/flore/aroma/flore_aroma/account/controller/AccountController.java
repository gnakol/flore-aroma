package fr.ecommerce.flore.aroma.flore_aroma.account.controller;

import fr.ecommerce.flore.aroma.flore_aroma.account.dto.LoginRequest;
import fr.ecommerce.flore.aroma.flore_aroma.account.dto.RegisterRequest;
import fr.ecommerce.flore.aroma.flore_aroma.account.dto.RegisterResponse;
import fr.ecommerce.flore.aroma.flore_aroma.account.dto.TokenResponse;
import fr.ecommerce.flore.aroma.flore_aroma.account.service.AccountService;
import fr.ecommerce.flore.aroma.flore_aroma.keycloak.service.KeycloakTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    private final KeycloakTokenService tokenService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest req) {
        // Ici on ne traite que le “non-Afrique” (email-first)
        RegisterResponse resp = accountService.registerNonAfricaEmailFlow(req);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/register-africa")
    public ResponseEntity<RegisterResponse> registerAfrica(@RequestBody RegisterRequest req) {
        return ResponseEntity.ok(accountService.registerAfricaPhoneFlow(req));
    }

    @GetMapping("/verify-phone")
    public ResponseEntity<String> verifyPhone(@RequestParam("code") String code) {
        accountService.confirmPhone(code);
        return ResponseEntity.ok("Phone verified! You can now log in.");
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest loginRequest) {
        String token = tokenService.authenticateUser(loginRequest);

        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }

        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("token", token); // Utilisez la clé "token"
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestParam("refresh_token") String refreshToken) {
        Map<String, Object> tokens = tokenService.refresh(refreshToken);
        return ResponseEntity.ok(new TokenResponse(tokens));
    }

    @DeleteMapping("/remove-account/{idAccount}")
    public ResponseEntity<String> removeAccount(@PathVariable Long idAccount) {
        this.accountService.removeAccount(idAccount);
        return ResponseEntity.status(200).body("Account with ID : " + idAccount + " was successfully remove");
    }


    @DeleteMapping("/remove-local-account/{idAccount}")
    public ResponseEntity<String> removeLocal(@Validated  @PathVariable Long idAccount)
    {
        this.accountService.removeLocal(idAccount);

        return ResponseEntity.status(200).body("Account with ID : "+idAccount+" was successfully remove");
    }
}

