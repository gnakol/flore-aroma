package fr.ecommerce.flore.aroma.flore_aroma.account.dto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDTO {

    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private String phoneE164;

    private String countryCode;

    private String accountType = "CUSTOMER";

    private boolean active = true;

    private String keycloakUserId;

    private OffsetDateTime createdAt = OffsetDateTime.now();

    private OffsetDateTime updatedAt = OffsetDateTime.now();

}