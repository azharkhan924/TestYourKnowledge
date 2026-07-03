package com.testplatform.repository;

import com.testplatform.model.Test;
import com.testplatform.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TestRepository extends JpaRepository<Test, Long> {
    List<Test> findByTeacherOrderByIdDesc(Teacher teacher);
}
