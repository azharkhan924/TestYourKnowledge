package com.testplatform.service;

import com.testplatform.dto.JoinRequest;
import com.testplatform.dto.SubmitRequest;
import com.testplatform.exception.ApiException;
import com.testplatform.model.*;
import com.testplatform.repository.AnswerRepository;
import com.testplatform.repository.AttemptRepository;
import com.testplatform.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AttemptService {

    public static final int TAB_SWITCH_LIMIT = 3; // 2 warnings, auto-submit on the 3rd

    private final AttemptRepository attemptRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final TestService testService;

    public AttemptService(AttemptRepository attemptRepository, QuestionRepository questionRepository,
                           AnswerRepository answerRepository, TestService testService) {
        this.attemptRepository = attemptRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.testService = testService;
    }

    public Attempt join(JoinRequest request) {
        if (request.getStudentName() == null || request.getStudentName().isBlank()) {
            throw ApiException.badRequest("Name is required");
        }
        if (request.getContactNumber() == null || request.getContactNumber().isBlank()) {
            throw ApiException.badRequest("Contact number is required");
        }
        Test test = testService.getTestOrThrow(request.getTestId());
        if (!test.isActive()) {
            throw ApiException.badRequest("This test is not currently accepting participants");
        }

        Attempt attempt = new Attempt();
        attempt.setTest(test);
        attempt.setStudentName(request.getStudentName().strip());
        attempt.setContactNumber(request.getContactNumber().strip());
        attempt.setStatus(AttemptStatus.PENDING);
        return attemptRepository.save(attempt);
    }

    public Attempt getAttemptOrThrow(Long attemptId) {
        return attemptRepository.findById(attemptId)
                .orElseThrow(() -> ApiException.notFound("Attempt not found"));
    }

    public List<Attempt> getWaitingRoom(Long testId) {
        Test test = testService.getTestOrThrow(testId);
        return attemptRepository.findByTestAndStatusOrderByJoinedAtAsc(test, AttemptStatus.PENDING);
    }

    public List<Attempt> getAllAttemptsForTest(Long testId) {
        Test test = testService.getTestOrThrow(testId);
        return attemptRepository.findByTestOrderByJoinedAtDesc(test);
    }

    public Attempt approve(Long attemptId) {
        Attempt attempt = getAttemptOrThrow(attemptId);
        if (attempt.getStatus() != AttemptStatus.PENDING) {
            throw ApiException.badRequest("Only pending attempts can be approved");
        }
        attempt.setStatus(AttemptStatus.APPROVED);
        attempt.setStartedAt(LocalDateTime.now());
        return attemptRepository.save(attempt);
    }

    public Attempt reject(Long attemptId) {
        Attempt attempt = getAttemptOrThrow(attemptId);
        if (attempt.getStatus() != AttemptStatus.PENDING) {
            throw ApiException.badRequest("Only pending attempts can be rejected");
        }
        attempt.setStatus(AttemptStatus.REJECTED);
        return attemptRepository.save(attempt);
    }

    public int approveAll(Long testId) {
        List<Attempt> pending = getWaitingRoom(testId);
        LocalDateTime now = LocalDateTime.now();
        for (Attempt a : pending) {
            a.setStatus(AttemptStatus.APPROVED);
            a.setStartedAt(now);
        }
        attemptRepository.saveAll(pending);
        return pending.size();
    }

    public List<Question> getQuestionsForAttempt(Long attemptId) {
        Attempt attempt = getAttemptOrThrow(attemptId);
        if (attempt.getStatus() != AttemptStatus.APPROVED) {
            throw ApiException.forbidden("Attempt is not approved yet");
        }
        return questionRepository.findByTestOrderByIdAsc(attempt.getTest());
    }

    public int recordViolation(Long attemptId) {
        Attempt attempt = getAttemptOrThrow(attemptId);
        if (attempt.getStatus() != AttemptStatus.APPROVED) {
            throw ApiException.badRequest("Attempt is not currently in progress");
        }
        int count = (attempt.getViolationCount() == null ? 0 : attempt.getViolationCount()) + 1;
        attempt.setViolationCount(count);
        attemptRepository.save(attempt);
        return count;
    }

    public Attempt submit(Long attemptId, SubmitRequest request) {
        Attempt attempt = getAttemptOrThrow(attemptId);
        if (attempt.getStatus() != AttemptStatus.APPROVED) {
            throw ApiException.badRequest("Attempt cannot be submitted in its current state");
        }

        List<Question> questions = questionRepository.findByTestOrderByIdAsc(attempt.getTest());
        Map<Long, String> submitted = request.getAnswers() == null ? Map.of() : request.getAnswers();

        int score = 0;
        for (Question q : questions) {
            String selected = submitted.get(q.getId());
            Answer answer = new Answer();
            answer.setAttempt(attempt);
            answer.setQuestion(q);
            answer.setSelectedOption(selected);
            answerRepository.save(answer);
            if (selected != null && selected.equalsIgnoreCase(q.getCorrectOption())) {
                score++;
            }
        }

        attempt.setScore(score);
        attempt.setTotalQuestions(questions.size());
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setEndReason(request.getEndReason() == null ? "MANUAL" : request.getEndReason());
        attempt.setStatus(AttemptStatus.SUBMITTED);
        return attemptRepository.save(attempt);
    }
}
