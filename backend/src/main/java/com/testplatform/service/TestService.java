package com.testplatform.service;

import com.testplatform.dto.CreateTestRequest;
import com.testplatform.dto.QuestionDto;
import com.testplatform.exception.ApiException;
import com.testplatform.model.Question;
import com.testplatform.model.Test;
import com.testplatform.model.Teacher;
import com.testplatform.repository.QuestionRepository;
import com.testplatform.repository.TestRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class TestService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;

    public TestService(TestRepository testRepository, QuestionRepository questionRepository) {
        this.testRepository = testRepository;
        this.questionRepository = questionRepository;
    }

    public Test createFromJson(CreateTestRequest request, Teacher teacher) {
        validateTestMeta(request.getTitle(), request.getDurationMinutes());
        if (request.getQuestions() == null || request.getQuestions().isEmpty()) {
            throw ApiException.badRequest("At least one question is required");
        }

        Test test = new Test();
        test.setTitle(request.getTitle());
        test.setDescription(request.getDescription());
        test.setDurationMinutes(request.getDurationMinutes());
        test.setTeacher(teacher);
        Test saved = testRepository.save(test);

        for (QuestionDto dto : request.getQuestions()) {
            saved.getQuestions().add(toEntity(dto, saved));
        }
        return testRepository.save(saved);
    }

    public Test createFromTextUpload(String title, String description, Integer durationMinutes, MultipartFile file, Teacher teacher) {
        validateTestMeta(title, durationMinutes);
        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw ApiException.badRequest("Could not read uploaded file");
        }
        List<QuestionDto> parsed = QuestionTextParser.parse(content);

        Test test = new Test();
        test.setTitle(title);
        test.setDescription(description);
        test.setDurationMinutes(durationMinutes);
        test.setTeacher(teacher);
        Test saved = testRepository.save(test);

        for (QuestionDto dto : parsed) {
            saved.getQuestions().add(toEntity(dto, saved));
        }
        return testRepository.save(saved);
    }

    public List<Test> getTestsByTeacher(Teacher teacher) {
        return testRepository.findByTeacherOrderByIdDesc(teacher);
    }

    public Test getTestOrThrow(Long id) {
        return testRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Test not found"));
    }

    public List<Question> getQuestionsForAdmin(Long testId) {
        Test test = getTestOrThrow(testId);
        return questionRepository.findByTestOrderByIdAsc(test);
    }

    private void validateTestMeta(String title, Integer durationMinutes) {
        if (title == null || title.isBlank()) throw ApiException.badRequest("Test title is required");
        if (durationMinutes == null || durationMinutes <= 0) throw ApiException.badRequest("Duration must be a positive number of minutes");
    }

    private Question toEntity(QuestionDto dto, Test test) {
        if (dto.getText() == null || dto.getText().isBlank()) throw ApiException.badRequest("A question is missing its text");
        if (dto.getOptionA() == null || dto.getOptionB() == null || dto.getOptionC() == null || dto.getOptionD() == null) {
            throw ApiException.badRequest("Every question needs options A, B, C and D");
        }
        if (dto.getCorrectOption() == null || !List.of("A", "B", "C", "D").contains(dto.getCorrectOption().toUpperCase())) {
            throw ApiException.badRequest("Every question needs a valid correctOption (A/B/C/D)");
        }
        Question q = new Question();
        q.setTest(test);
        q.setText(dto.getText());
        q.setOptionA(dto.getOptionA());
        q.setOptionB(dto.getOptionB());
        q.setOptionC(dto.getOptionC());
        q.setOptionD(dto.getOptionD());
        q.setCorrectOption(dto.getCorrectOption().toUpperCase());
        return q;
    }
}
