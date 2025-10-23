package vn.id.nhanbe.ibanking.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vn.id.nhanbe.ibanking.model.User;
import vn.id.nhanbe.ibanking.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ObjectProvider<HttpServletRequest> requestProvider;

    public User getCurrentUser() {
        HttpServletRequest request = requestProvider.getIfAvailable();
        if (request == null) {
            throw new BadCredentialsException("Missing request context");
        }

        String token = resolveToken(request);
        String username = jwtService.extractSubject(token);

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            throw new BadCredentialsException("Missing Authorization header");
        }

        if (!authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new BadCredentialsException("Invalid Authorization header");
        }

        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            throw new BadCredentialsException("Invalid Authorization header");
        }

        return token;
    }
}
