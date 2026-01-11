package com.eshop.app.service;

import com.eshop.app.dto.request.*;
import com.eshop.app.dto.response.*;

import java.util.List;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse register(RegisterRequest request, String clientIp);

    AuthResponse login(LoginRequest request);
    AuthResponse login(LoginRequest request, String clientIp);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(String refreshToken, Long userId);
    void logoutAll(Long userId);

    UserResponse getCurrentUser();
    UserResponse getCurrentUser(Long userId);

    void forgotPassword(String email);
    void resetPassword(ResetPasswordRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);

    void verifyEmail(String token);
    void resendVerificationEmail(Long userId);

    TwoFactorSetupResponse enableTwoFactor(Long userId);
    void verifyTwoFactorSetup(Long userId, String code);
    void disableTwoFactor(Long userId, String password, String code);
    AuthResponse validateTwoFactorLogin(TwoFactorLoginRequest request);

    List<SessionResponse> getActiveSessions(Long userId);
    void revokeSession(Long userId, String sessionId);
}
