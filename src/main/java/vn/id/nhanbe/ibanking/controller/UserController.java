package vn.id.nhanbe.ibanking.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.id.nhanbe.ibanking.dto.UserInfoResponse;
import vn.id.nhanbe.ibanking.service.UserService;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserInfoResponse me() {
        return userService.getUserInfo();
    }
}
