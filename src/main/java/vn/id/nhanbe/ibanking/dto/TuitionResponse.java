package vn.id.nhanbe.ibanking.dto;

import java.math.BigDecimal;

public record TuitionResponse(String studentId, String fullName, BigDecimal amount, String term, String status) {
}
