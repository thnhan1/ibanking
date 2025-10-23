package vn.id.nhanbe.ibanking.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionHistoryItem(String referenceCode, BigDecimal amount, String status, Instant createdAt) {
}
