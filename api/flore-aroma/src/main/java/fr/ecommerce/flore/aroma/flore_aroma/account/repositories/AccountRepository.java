package fr.ecommerce.flore.aroma.flore_aroma.account.repositories;

import fr.ecommerce.flore.aroma.flore_aroma.account.bean.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByKeycloakUserId(String keycloakUserId);

    // retrouver aussi par téléphone
    Optional<Account> findByPhoneE164(String phoneE164);
}
