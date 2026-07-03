package com.testplatform.controller;

import com.testplatform.dto.JoinRequest;
import com.testplatform.dto.QuestionDto;
import com.testplatform.dto.SubmitRequest;
import com.testplatform.dto.ViolationRequest;
import com.testplatform.exception.ApiException;
import com.testplatform.model.Attempt;
import com.testplatform.model.Question;
import com.testplatform.model.Test;
import com.testplatform.service.AttemptService;
import com.testplatform.service.TestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class StudentController {

    private final TestService testService;
    private final AttemptService attemptService;

    public StudentController(TestService testService, AttemptService attemptService) {
        this.testService = testService;
        this.attemptService = attemptService;
    }

    @GetMapping("/tests/{id}")
    public ResponseEntity<?> getPublicTestInfo(@PathVariable Long id) {
        Test test = testService.getTestOrThrow(id);
        if (!test.isActive()) {
            throw ApiException.badRequest("This test is not currently accepting participants");
        }
        return ResponseEntity.ok(Map.of(
                "id", test.getId(),
                "title", test.getTitle(),
                "description", test.getDescription() == null ? "" : test.getDescription(),
                "durationMinutes", test.getDurationMinutes(),
                "questionCount", test.getQuestions().size()
        ));
    }

    @PostMapping("/student/join")
    public ResponseEntity<?> join(@RequestBody JoinRequest request) {
        Attempt attempt = attemptService.join(request);
        return ResponseEntity.ok(Map.of(
                "attemptId", attempt.getId(),
                "status", attempt.getStatus().name()
        ));
    }

    @GetMapping("/student/attempts/{id}/status")
    public ResponseEntity<?> getStatus(@PathVariable Long id) {
        Attempt attempt = attemptService.getAttemptOrThrow(id);
        return ResponseEntity.ok(Map.of(
                "status", attempt.getStatus().name(),
                "durationMinutes", attempt.getTest().getDurationMinutes()
        ));
    }

    @GetMapping("/student/attempts/{id}/questions")
    public ResponseEntity<List<QuestionDto>> getQuestions(@PathVariable Long id) {
        List<Question> questions = attemptService.getQuestionsForAttempt(id);
        List<QuestionDto> safe = questions.stream().map(q -> {
            QuestionDto dto = new QuestionDto();
            dto.setId(q.getId());
            dto.setText(q.getText());
            dto.setOptionA(q.getOptionA());
            dto.setOptionB(q.getOptionB());
            dto.setOptionC(q.getOptionC());
            dto.setOptionD(q.getOptionD());
            // correctOption intentionally left null — never expose to student
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(safe);
    }

    @PostMapping("/student/attempts/{id}/violation")
    public ResponseEntity<?> recordViolation(@PathVariable Long id, @RequestBody ViolationRequest request) {
        int count = attemptService.recordViolation(id);
        boolean limitReached = count >= AttemptService.TAB_SWITCH_LIMIT;
        return ResponseEntity.ok(Map.of("violationCount", count, "limitReached", limitReached));
    }

    @PostMapping("/student/attempts/{id}/submit")
    public ResponseEntity<?> submit(@PathVariable Long id, @RequestBody SubmitRequest request) {
        Attempt attempt = attemptService.submit(id, request);
        return ResponseEntity.ok(Map.of(
                "score", attempt.getScore(),
                "total", attempt.getTotalQuestions()
        ));
    }
}
