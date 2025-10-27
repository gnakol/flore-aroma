package fr.ecommerce.flore.aroma.flore_aroma.whatsapp.repositories;

import fr.ecommerce.flore.aroma.flore_aroma.whatsapp.bean.PhoneVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {
    Optional<PhoneVerification> findByCodeAndUsedFalse(String code);
}
