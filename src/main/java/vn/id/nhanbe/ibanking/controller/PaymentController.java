package vn.id.nhanbe.ibanking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import vn.id.nhanbe.ibanking.dto.ConfirmPaymentRequest;
import vn.id.nhanbe.ibanking.dto.ConfirmPaymentResponse;
import vn.id.nhanbe.ibanking.dto.CreatePaymentSessionRequest;
import vn.id.nhanbe.ibanking.dto.PaymentSessionResponse;
import vn.id.nhanbe.ibanking.dto.ResendOtpResponse;
import vn.id.nhanbe.ibanking.dto.TransactionHistoryResponse;
import vn.id.nhanbe.ibanking.service.PaymentService;

import java.time.Instant;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/session")
    public PaymentSessionResponse createSession(@Valid @RequestBody CreatePaymentSessionRequest request) {
        return paymentService.createSession(request);
    }

    @PostMapping("/confirm")
    public ConfirmPaymentResponse confirm(@Valid @RequestBody ConfirmPaymentRequest request) {
        return paymentService.confirmPayment(request);
    }

    @GetMapping("/transactions")
    public TransactionHistoryResponse listTransactions(
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return paymentService.getTransactions(parseInstant(from), parseInstant(to), pageable);
    }

    @PostMapping("/{txId}/otp/resend")
    public ResendOtpResponse resendOtp(@PathVariable String txId) {
        return paymentService.resendOtp(txId);
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid datetime format");
        }
    }
}
