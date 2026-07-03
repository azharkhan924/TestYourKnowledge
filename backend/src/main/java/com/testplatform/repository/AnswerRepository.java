package com.testplatform.repository;

import com.testplatform.model.Answer;
import com.testplatform.model.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findByAttempt(Attempt attempt);
}
