package com.testplatform.config;

import com.testplatform.model.Teacher;
import com.testplatform.repository.TeacherRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private AdminTokenStore tokenStore;

    @Autowired
    private TeacherRepository teacherRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        // Login and OTP request endpoints must remain open
        if (path.endsWith("/api/admin/login") || path.endsWith("/api/admin/request-otp")) {
            return true;
        }
        String header = request.getHeader("Authorization");
        String token = (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
        if (tokenStore.isValid(token)) {
            String email = tokenStore.getEmail(token);
            if (email != null) {
                Teacher teacher = teacherRepository.findByEmail(email).orElse(null);
                if (teacher != null) {
                    request.setAttribute("currentTeacher", teacher);
                    return true;
                }
            }
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized. Please log in again.\"}");
        return false;
    }
}
