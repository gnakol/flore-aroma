package fr.ecommerce.flore.aroma.flore_aroma.account.bean;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "account")
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_account")
    private Long id;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "email", length = 255, unique = true)
    private String email;

    @Column(name = "phone_e164", length = 20, nullable = false, unique = true)
    private String phoneE164;

    @Column(name = "country_code", length = 10, nullable = false)
    private String countryCode;

    @Column(name = "account_type", length = 20, nullable = false)
    private String accountType = "CUSTOMER";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "keycloak_user_id", length = 255, unique = true)
    private String keycloakUserId;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}

