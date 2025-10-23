package vn.id.nhanbe.ibanking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.id.nhanbe.ibanking.dto.UserInfoResponse;
import vn.id.nhanbe.ibanking.repository.AccountRepository;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UserService {

    private final CurrentUserService currentUserService;
    private final AccountRepository accountRepository;

    public UserInfoResponse getUserInfo() {
        var user = currentUserService.getCurrentUser();
        BigDecimal balance = accountRepository.findFirstByUserUsername(user.getUsername())
                .map(account -> account.getBalance())
                .orElse(BigDecimal.ZERO);

        return new UserInfoResponse(
                user.getFullName(),
                user.getPhone(),
                user.getEmail(),
                balance
        );
    }
}
