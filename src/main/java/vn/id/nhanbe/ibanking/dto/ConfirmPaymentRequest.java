package vn.id.nhanbe.ibanking.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmPaymentRequest(
        @NotBlank String txId,
        @NotBlank String otp
) {
}
