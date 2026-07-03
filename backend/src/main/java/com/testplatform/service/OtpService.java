package com.testplatform.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final int OTP_EXPIRY_MINUTES = 5;
    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public static class OtpData {
        private final String code;
        private final LocalDateTime expiresAt;

        public OtpData(String code, LocalDateTime expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }

        public String getCode() {
            return code;
        }

        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }
    }

    public String generateOtp(String email) {
        String code = String.format("%06d", random.nextInt(1000000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        otpStorage.put(email, new OtpData(code, expiresAt));
        return code;
    }

    public boolean verifyOtp(String email, String inputCode) {
        if (email == null || inputCode == null) {
            return false;
        }
        OtpData data = otpStorage.get(email);
        if (data == null) {
            return false;
        }
        if (LocalDateTime.now().isAfter(data.getExpiresAt())) {
            otpStorage.remove(email);
            return false;
        }
        boolean matches = data.getCode().equals(inputCode);
        if (matches) {
            otpStorage.remove(email); // consume OTP on successful verification
        }
        return matches;
    }
}
