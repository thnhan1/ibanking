package vn.id.nhanbe.ibanking.dto;

import java.math.BigDecimal;

public record UserInfoResponse(String fullName, String phone, String email, BigDecimal balance) {
}
