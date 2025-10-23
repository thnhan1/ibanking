package vn.id.nhanbe.ibanking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.id.nhanbe.ibanking.model.OtpToken;

import java.util.Optional;
import java.util.UUID;

public interface OtpTokenRepository extends JpaRepository<OtpToken, UUID> {

    Optional<OtpToken> findByTransactionId(UUID transactionId);

    Optional<OtpToken> findByTransactionIdAndUsedFalse(UUID transactionId);
}
