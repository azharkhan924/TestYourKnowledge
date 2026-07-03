package com.testplatform.controller;

import com.testplatform.dto.CreateTestRequest;
import com.testplatform.dto.LoginRequest;
import com.testplatform.dto.OtpRequest;
import com.testplatform.model.Attempt;
import com.testplatform.model.Question;
import com.testplatform.model.Test;
import com.testplatform.model.Teacher;
import com.testplatform.service.AdminAuthService;
import com.testplatform.service.AttemptService;
import com.testplatform.service.TestService;
import com.testplatform.exception.ApiException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminAuthService adminAuthService;
    private final TestService testService;
    private final AttemptService attemptService;

    public AdminController(AdminAuthService adminAuthService, TestService testService, AttemptService attemptService) {
        this.adminAuthService = adminAuthService;
        this.testService = testService;
        this.attemptService = attemptService;
    }

    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOtp(@RequestBody OtpRequest request) {
        adminAuthService.requestOtp(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String token = adminAuthService.login(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/tests")
    public ResponseEntity<Test> createTestFromJson(@RequestBody CreateTestRequest request,
                                                    @RequestAttribute("currentTeacher") Teacher teacher) {
        return ResponseEntity.ok(testService.createFromJson(request, teacher));
    }

    @PostMapping(value = "/tests/upload", consumes = "multipart/form-data")
    public ResponseEntity<Test> createTestFromUpload(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("durationMinutes") Integer durationMinutes,
            @RequestParam("file") MultipartFile file,
            @RequestAttribute("currentTeacher") Teacher teacher) {
        return ResponseEntity.ok(testService.createFromTextUpload(title, description, durationMinutes, file, teacher));
    }

    @GetMapping("/tests")
    public ResponseEntity<List<Test>> getAllTests(@RequestAttribute("currentTeacher") Teacher teacher) {
        return ResponseEntity.ok(testService.getTestsByTeacher(teacher));
    }

    @GetMapping("/tests/{id}/questions")
    public ResponseEntity<List<Question>> getQuestions(@PathVariable Long id,
                                                       @RequestAttribute("currentTeacher") Teacher teacher) {
        checkOwnership(id, teacher);
        return ResponseEntity.ok(testService.getQuestionsForAdmin(id));
    }

    @GetMapping("/tests/{id}/waiting-room")
    public ResponseEntity<List<Attempt>> getWaitingRoom(@PathVariable Long id,
                                                        @RequestAttribute("currentTeacher") Teacher teacher) {
        checkOwnership(id, teacher);
        return ResponseEntity.ok(attemptService.getWaitingRoom(id));
    }

    @PostMapping("/tests/{id}/waiting-room/approve-all")
    public ResponseEntity<?> approveAll(@PathVariable Long id,
                                        @RequestAttribute("currentTeacher") Teacher teacher) {
        checkOwnership(id, teacher);
        int count = attemptService.approveAll(id);
        return ResponseEntity.ok(Map.of("approvedCount", count));
    }

    @PostMapping("/attempts/{attemptId}/approve")
    public ResponseEntity<Attempt> approve(@PathVariable Long attemptId,
                                           @RequestAttribute("currentTeacher") Teacher teacher) {
        checkAttemptOwnership(attemptId, teacher);
        return ResponseEntity.ok(attemptService.approve(attemptId));
    }

    @PostMapping("/attempts/{attemptId}/reject")
    public ResponseEntity<Attempt> reject(@PathVariable Long attemptId,
                                          @RequestAttribute("currentTeacher") Teacher teacher) {
        checkAttemptOwnership(attemptId, teacher);
        return ResponseEntity.ok(attemptService.reject(attemptId));
    }

    @GetMapping("/tests/{id}/results")
    public ResponseEntity<List<Attempt>> getResults(@PathVariable Long id,
                                                    @RequestAttribute("currentTeacher") Teacher teacher) {
        checkOwnership(id, teacher);
        return ResponseEntity.ok(attemptService.getAllAttemptsForTest(id));
    }

    private void checkOwnership(Long testId, Teacher teacher) {
        Test test = testService.getTestOrThrow(testId);
        if (test.getTeacher() == null || !test.getTeacher().getId().equals(teacher.getId())) {
            throw ApiException.forbidden("You do not have permission to access this test");
        }
    }

    private void checkAttemptOwnership(Long attemptId, Teacher teacher) {
        Attempt attempt = attemptService.getAttemptOrThrow(attemptId);
        if (attempt.getTest().getTeacher() == null || !attempt.getTest().getTeacher().getId().equals(teacher.getId())) {
            throw ApiException.forbidden("You do not have permission to access this attempt");
        }
    }
}
