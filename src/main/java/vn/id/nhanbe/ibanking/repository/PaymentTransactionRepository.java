package vn.id.nhanbe.ibanking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.id.nhanbe.ibanking.model.Account;
import vn.id.nhanbe.ibanking.model.PaymentTransaction;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    Optional<PaymentTransaction> findByPayerAccountAndIdempotencyKey(Account payerAccount, String idempotencyKey);

    Page<PaymentTransaction> findByPayerAccount(Account payerAccount, Pageable pageable);

    Page<PaymentTransaction> findByPayerAccountAndCreatedAtBetween(Account payerAccount, Instant from, Instant to, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PaymentTransaction t where t.id = :id")
    Optional<PaymentTransaction> findByIdForUpdate(@Param("id") UUID id);
}
