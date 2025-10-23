package vn.id.nhanbe.ibanking.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.id.nhanbe.ibanking.dto.TuitionResponse;
import vn.id.nhanbe.ibanking.service.TuitionService;

@RestController
@RequestMapping("/api/v1/tuituion")
@RequiredArgsConstructor
public class TuitionController {

    private final TuitionService tuitionService;

    @GetMapping("/{studentId}")
    public TuitionResponse queryTuition(@PathVariable String studentId, @RequestParam String term) {
        return tuitionService.getTuition(studentId, term);
    }
}
