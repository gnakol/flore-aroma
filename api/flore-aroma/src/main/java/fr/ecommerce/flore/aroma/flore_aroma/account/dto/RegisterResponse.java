package fr.ecommerce.flore.aroma.flore_aroma.account.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterResponse {
    private Long accountId;
    private String keycloakUserId;
    private boolean emailVerificationSent;
}

