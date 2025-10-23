package vn.id.nhanbe.ibanking.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.id.nhanbe.ibanking.dto.ConfirmPaymentRequest;
import vn.id.nhanbe.ibanking.dto.ConfirmPaymentResponse;
import vn.id.nhanbe.ibanking.dto.CreatePaymentSessionRequest;
import vn.id.nhanbe.ibanking.dto.PaymentSessionResponse;
import vn.id.nhanbe.ibanking.dto.ResendOtpResponse;
import vn.id.nhanbe.ibanking.dto.TransactionHistoryItem;
import vn.id.nhanbe.ibanking.dto.TransactionHistoryResponse;
import vn.id.nhanbe.ibanking.model.Account;
import vn.id.nhanbe.ibanking.model.OtpToken;
import vn.id.nhanbe.ibanking.model.PaymentTransaction;
import vn.id.nhanbe.ibanking.model.TransactionStatus;
import vn.id.nhanbe.ibanking.model.TuitionFee;
import vn.id.nhanbe.ibanking.model.TuitionStatus;
import vn.id.nhanbe.ibanking.repository.AccountRepository;
import vn.id.nhanbe.ibanking.repository.OtpTokenRepository;
import vn.id.nhanbe.ibanking.repository.PaymentTransactionRepository;
import vn.id.nhanbe.ibanking.repository.TuitionFeeRepository;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final int OTP_LENGTH = 6;
    private static final String OTP_CHANNEL = "EMAIL";

    private final SecureRandom secureRandom = new SecureRandom();

    private final CurrentUserService currentUserService;
    private final AccountRepository accountRepository;
    private final TuitionFeeRepository tuitionFeeRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final OtpNotificationService otpNotificationService;

    @Transactional
    public PaymentSessionResponse createSession(CreatePaymentSessionRequest request) {
        var user = currentUserService.getCurrentUser();
        var account = accountRepository.findFirstByUserUsername(user.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account not found"));

        TuitionFee tuitionFee = tuitionFeeRepository.findByStudentCodeAndTerm(request.studentId(), request.term())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tuition not found"));

        validateAmountMatchesTuition(request.amount(), tuitionFee);
        ensureTuitionNotPaid(tuitionFee);

        return paymentTransactionRepository.findByPayerAccountAndIdempotencyKey(account, request.idempotencyKey())
                .map(existing -> {
                    verifyExistingTransaction(existing, tuitionFee, request.amount());
                    if (existing.getStatus() != TransactionStatus.SUCCESS) {
                        issueOtp(existing);
                        existing.setStatus(TransactionStatus.OTP_SENT);
                        paymentTransactionRepository.save(existing);
                    }
                    log.debug("Reusing existing transaction {} for idempotency key {}", existing.getId(), request.idempotencyKey());
                    return new PaymentSessionResponse(existing.getId().toString(), existing.getStatus().name());
                })
                .orElseGet(() -> createNewSession(account, tuitionFee, request.amount(), request.idempotencyKey()));
    }

    @Transactional
    public ConfirmPaymentResponse confirmPayment(ConfirmPaymentRequest request) {
        var user = currentUserService.getCurrentUser();
        PaymentTransaction transaction = getTransactionForUser(request.txId(), user.getUsername(), true);

        if (transaction.getStatus() == TransactionStatus.SUCCESS) {
            return new ConfirmPaymentResponse(
                    transaction.getStatus().name(),
                    transaction.getReferenceCode(),
                    transaction.getPayerAccount().getBalance()
            );
        }

        if (transaction.getStatus() != TransactionStatus.OTP_SENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transaction is not awaiting confirmation");
        }

        OtpToken otpToken = otpTokenRepository.findByTransactionId(transaction.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP not found"));

        validateOtp(otpToken, request.otp());

        Account account = transaction.getPayerAccount();
        if (account.getBalance().compareTo(transaction.getAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        otpToken.setUsed(true);
        otpToken.setExpiresAt(Instant.now());

        account.setBalance(account.getBalance().subtract(transaction.getAmount()));
        TuitionFee tuitionFee = transaction.getTuitionFee();
        tuitionFee.setStatus(TuitionStatus.PAID);
        transaction.setStatus(TransactionStatus.SUCCESS);
        paymentTransactionRepository.save(transaction);

        log.info("Transaction {} confirmed successfully with reference {}", transaction.getId(), transaction.getReferenceCode());

        return new ConfirmPaymentResponse(
                transaction.getStatus().name(),
                transaction.getReferenceCode(),
                account.getBalance()
        );
    }

    @Transactional(readOnly = true)
    public TransactionHistoryResponse getTransactions(Instant from, Instant to, Pageable pageable) {
        var user = currentUserService.getCurrentUser();
        var account = accountRepository.findFirstByUserUsername(user.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account not found"));

        Instant effectiveFrom = from != null ? from : Instant.EPOCH;
        Instant effectiveTo = to != null ? to : Instant.now();

        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "`from` must be before `to`");
        }

        Page<PaymentTransaction> page;
        if (from == null && to == null) {
            page = paymentTransactionRepository.findByPayerAccount(account, pageable);
        } else {
            page = paymentTransactionRepository.findByPayerAccountAndCreatedAtBetween(account, effectiveFrom, effectiveTo, pageable);
        }

        List<TransactionHistoryItem> items = page.map(tx -> new TransactionHistoryItem(
                tx.getReferenceCode(),
                tx.getAmount(),
                tx.getStatus().name(),
                tx.getCreatedAt()
        )).getContent();

        return new TransactionHistoryResponse(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Transactional
    public ResendOtpResponse resendOtp(String txId) {
        var user = currentUserService.getCurrentUser();
        PaymentTransaction transaction = getTransactionForUser(txId, user.getUsername());

        if (transaction.getStatus() == TransactionStatus.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transaction already completed");
        }

        issueOtp(transaction);
        transaction.setStatus(TransactionStatus.OTP_SENT);
        paymentTransactionRepository.save(transaction);

        return new ResendOtpResponse(TransactionStatus.OTP_SENT.name());
    }

    private PaymentSessionResponse createNewSession(Account account, TuitionFee tuitionFee, BigDecimal amount, String idempotencyKey) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setPayerAccount(account);
        transaction.setStudent(tuitionFee.getStudent());
        transaction.setTuitionFee(tuitionFee);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.OTP_SENT);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setReferenceCode(generateReferenceCode());

        paymentTransactionRepository.save(transaction);
        issueOtp(transaction);

        log.info("Created payment transaction {} for student {} term {}", transaction.getId(), tuitionFee.getStudent().getStudentCode(), tuitionFee.getTerm());

        return new PaymentSessionResponse(transaction.getId().toString(), transaction.getStatus().name());
    }

    private PaymentTransaction getTransactionForUser(String txId, String username) {
        return getTransactionForUser(txId, username, false);
    }

    private PaymentTransaction getTransactionForUser(String txId, String username, boolean forUpdate) {
        UUID uuid = parseUuid(txId);
        PaymentTransaction transaction = (forUpdate
                ? paymentTransactionRepository.findByIdForUpdate(uuid)
                : paymentTransactionRepository.findById(uuid))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        var accountUser = transaction.getPayerAccount().getUser();
        if (accountUser == null || !accountUser.getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Transaction not accessible");
        }

        return transaction;
    }

    private void validateAmountMatchesTuition(BigDecimal amount, TuitionFee tuitionFee) {
        if (amount == null || tuitionFee.getAmount().compareTo(amount) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must match tuition");
        }
    }

    private void ensureTuitionNotPaid(TuitionFee tuitionFee) {
        if (tuitionFee.getStatus() == TuitionStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tuition already paid");
        }
    }

    private void verifyExistingTransaction(PaymentTransaction transaction, TuitionFee tuitionFee, BigDecimal amount) {
        if (!transaction.getTuitionFee().getId().equals(tuitionFee.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key conflicts with another tuition");
        }
        if (transaction.getAmount().compareTo(amount) != 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key conflicts with another amount");
        }
    }

    private void issueOtp(PaymentTransaction transaction) {
        String otpCode = generateOtpCode();
        Instant now = Instant.now();

        OtpToken otpToken = otpTokenRepository.findByTransactionId(transaction.getId())
                .orElseGet(OtpToken::new);

        otpToken.setTransaction(transaction);
        otpToken.setChannel(OTP_CHANNEL);
        otpToken.setCode(otpCode);
        otpToken.setIssuedAt(now);
        otpToken.setExpiresAt(now.plus(OTP_TTL));
        otpToken.setUsed(false);

        otpTokenRepository.save(otpToken);

        var payer = transaction.getPayerAccount().getUser();
        if (payer == null || payer.getEmail() == null || payer.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing user email for OTP delivery");
        }

        try {
            otpNotificationService.sendOtp(payer.getEmail(), otpCode, transaction.getReferenceCode());
        } catch (MailException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Failed to send OTP", ex);
        }

        log.info("OTP issued for transaction {}", transaction.getId());
    }

    private void validateOtp(OtpToken otpToken, String providedOtp) {
        if (Boolean.TRUE.equals(otpToken.getUsed())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP already used");
        }
        if (Instant.now().isAfter(otpToken.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP expired");
        }
        if (!otpToken.getCode().equals(providedOtp)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP");
        }
    }

    private UUID parseUuid(String txId) {
        try {
            return UUID.fromString(txId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid transaction id");
        }
    }

    private String generateReferenceCode() {
        String raw = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return "TX-" + raw.substring(0, 12);
    }

    private String generateOtpCode() {
        int bound = (int) Math.pow(10, OTP_LENGTH);
        int number = secureRandom.nextInt(bound);
        return String.format("%0" + OTP_LENGTH + "d", number);
    }
}
