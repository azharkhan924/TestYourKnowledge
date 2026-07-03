package com.testplatform.service;

import com.testplatform.config.AdminTokenStore;
import com.testplatform.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    private final AdminTokenStore tokenStore;

    public AdminAuthService(AdminTokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    public String login(String username, String password) {
        if (username == null || password == null
                || !username.equals(adminUsername) || !password.equals(adminPassword)) {
            throw ApiException.unauthorized("Invalid username or password");
        }
        return tokenStore.issueToken();
    }
}
