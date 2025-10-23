package vn.id.nhanbe.ibanking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.id.nhanbe.ibanking.model.Student;

import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {
    Optional<Student> findByStudentCode(String studentCode);
}
