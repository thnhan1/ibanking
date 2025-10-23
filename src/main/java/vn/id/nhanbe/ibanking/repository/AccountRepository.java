package vn.id.nhanbe.ibanking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.id.nhanbe.ibanking.model.Account;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findFirstByUserUsername(String username);
}
