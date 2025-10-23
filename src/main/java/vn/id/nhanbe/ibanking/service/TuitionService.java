package vn.id.nhanbe.ibanking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.id.nhanbe.ibanking.dto.TuitionResponse;
import vn.id.nhanbe.ibanking.repository.TuitionFeeRepository;

@Service
@RequiredArgsConstructor
public class TuitionService {

    private final TuitionFeeRepository tuitionFeeRepository;

    @Transactional(readOnly = true)
    public TuitionResponse getTuition(String studentId, String term) {
        if (studentId == null || studentId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studentId is required");
        }
        if (term == null || term.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "term is required");
        }

        return tuitionFeeRepository.findByStudentCodeAndTerm(studentId, term)
                .map(tuition -> new TuitionResponse(
                        tuition.getStudent().getStudentCode(),
                        tuition.getStudent().getFullName(),
                        tuition.getAmount(),
                        tuition.getTerm(),
                        tuition.getStatus().name()
                ))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tuition not found"));
    }
}
