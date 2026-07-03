package com.testplatform.repository;

import com.testplatform.model.Attempt;
import com.testplatform.model.AttemptStatus;
import com.testplatform.model.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {
    List<Attempt> findByTestAndStatusOrderByJoinedAtAsc(Test test, AttemptStatus status);
    List<Attempt> findByTestOrderByJoinedAtDesc(Test test);
}
