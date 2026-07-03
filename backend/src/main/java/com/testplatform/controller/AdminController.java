package com.testplatform.controller;

import com.testplatform.dto.CreateTestRequest;
import com.testplatform.dto.LoginRequest;
import com.testplatform.model.Attempt;
import com.testplatform.model.Question;
import com.testplatform.model.Test;
import com.testplatform.service.AdminAuthService;
import com.testplatform.service.AttemptService;
import com.testplatform.service.TestService;
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

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String token = adminAuthService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/tests")
    public ResponseEntity<Test> createTestFromJson(@RequestBody CreateTestRequest request) {
        return ResponseEntity.ok(testService.createFromJson(request));
    }

    @PostMapping(value = "/tests/upload", consumes = "multipart/form-data")
    public ResponseEntity<Test> createTestFromUpload(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("durationMinutes") Integer durationMinutes,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(testService.createFromTextUpload(title, description, durationMinutes, file));
    }

    @GetMapping("/tests")
    public ResponseEntity<List<Test>> getAllTests() {
        return ResponseEntity.ok(testService.getAllTests());
    }

    @GetMapping("/tests/{id}/questions")
    public ResponseEntity<List<Question>> getQuestions(@PathVariable Long id) {
        return ResponseEntity.ok(testService.getQuestionsForAdmin(id));
    }

    @GetMapping("/tests/{id}/waiting-room")
    public ResponseEntity<List<Attempt>> getWaitingRoom(@PathVariable Long id) {
        return ResponseEntity.ok(attemptService.getWaitingRoom(id));
    }

    @PostMapping("/tests/{id}/waiting-room/approve-all")
    public ResponseEntity<?> approveAll(@PathVariable Long id) {
        int count = attemptService.approveAll(id);
        return ResponseEntity.ok(Map.of("approvedCount", count));
    }

    @PostMapping("/attempts/{attemptId}/approve")
    public ResponseEntity<Attempt> approve(@PathVariable Long attemptId) {
        return ResponseEntity.ok(attemptService.approve(attemptId));
    }

    @PostMapping("/attempts/{attemptId}/reject")
    public ResponseEntity<Attempt> reject(@PathVariable Long attemptId) {
        return ResponseEntity.ok(attemptService.reject(attemptId));
    }

    @GetMapping("/tests/{id}/results")
    public ResponseEntity<List<Attempt>> getResults(@PathVariable Long id) {
        return ResponseEntity.ok(attemptService.getAllAttemptsForTest(id));
    }
}
