package vn.id.nhanbe.ibanking.dto;

import java.math.BigDecimal;

public record ConfirmPaymentResponse(String status, String referenceCode, BigDecimal newBalance) {
}
