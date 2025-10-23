package vn.id.nhanbe.ibanking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.id.nhanbe.ibanking.model.TuitionFee;

import java.util.Optional;
import java.util.UUID;

public interface TuitionFeeRepository extends JpaRepository<TuitionFee, UUID> {

    @Query("select tf from TuitionFee tf join fetch tf.student s where s.studentCode = :studentCode and tf.term = :term")
    Optional<TuitionFee> findByStudentCodeAndTerm(@Param("studentCode") String studentCode, @Param("term") String term);
}
