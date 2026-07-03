package com.testplatform.service;

import com.testplatform.config.AdminTokenStore;
import com.testplatform.exception.ApiException;
import com.testplatform.model.Teacher;
import com.testplatform.repository.TeacherRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {

    private final AdminTokenStore tokenStore;
    private final OtpService otpService;
    private final EmailService emailService;
    private final TeacherRepository teacherRepository;

    public AdminAuthService(AdminTokenStore tokenStore, OtpService otpService,
                            EmailService emailService, TeacherRepository teacherRepository) {
        this.tokenStore = tokenStore;
        this.otpService = otpService;
        this.emailService = emailService;
        this.teacherRepository = teacherRepository;
    }

    public void requestOtp(String email) {
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            throw ApiException.badRequest("Please enter a valid email address");
        }
        String otp = otpService.generateOtp(email.trim().toLowerCase());
        emailService.sendOtp(email.trim().toLowerCase(), otp);
    }

    public String login(String email, String otp) {
        if (email == null || otp == null) {
            throw ApiException.unauthorized("Email and OTP are required");
        }
        String normalizedEmail = email.trim().toLowerCase();
        boolean verified = otpService.verifyOtp(normalizedEmail, otp.trim());
        if (!verified) {
            throw ApiException.unauthorized("Invalid or expired OTP");
        }

        // Automatically register teacher if they do not exist
        Teacher teacher = teacherRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> {
                    Teacher t = new Teacher();
                    t.setEmail(normalizedEmail);
                    return teacherRepository.save(t);
                });

        return tokenStore.issueToken(teacher.getEmail());
    }
}
