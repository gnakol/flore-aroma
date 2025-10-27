package fr.ecommerce.flore.aroma.flore_aroma.whatsapp.bean;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "phone_verification")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String keycloakUserId;

    @Column(nullable = false, unique = true)
    private String code; // token unique

    @Column(name = "phone_e164", nullable = false)
    private String phoneE164;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used;
}

