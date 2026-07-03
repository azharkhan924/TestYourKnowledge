package com.testplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public void sendOtp(String email, String otp) {
        System.out.println("\n=================================================");
        System.out.println("            [OTP VERIFICATION LOG]");
        System.out.println("  Teacher Email: " + email);
        System.out.println("  Generated OTP: " + otp);
        System.out.println("=================================================\n");

        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(email);
                message.setSubject("Your Test Platform OTP Verification Code");
                message.setText("Hello,\n\nYour OTP verification code for the Test Platform is: " + otp + "\n\nThis code will expire in 5 minutes.\n\nRegards,\nTest Platform Support");
                mailSender.send(message);
                System.out.println("OTP email successfully sent to " + email);
            } catch (Exception e) {
                System.err.println("WARNING: Failed to send email to " + email + ". Error: " + e.getMessage());
                System.err.println("Please check your SMTP properties in application.properties. OTP is displayed in the console above.");
            }
        } else {
            System.out.println("INFO: JavaMailSender is not configured. Falling back to console OTP logging.");
        }
    }
}
