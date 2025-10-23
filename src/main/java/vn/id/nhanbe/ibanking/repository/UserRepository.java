package vn.id.nhanbe.ibanking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.id.nhanbe.ibanking.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
}
