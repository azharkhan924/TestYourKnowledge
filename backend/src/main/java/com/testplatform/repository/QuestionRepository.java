package com.testplatform.repository;

import com.testplatform.model.Question;
import com.testplatform.model.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByTestOrderByIdAsc(Test test);
}
