package com.eshop.app.controller;

import com.eshop.app.dto.request.*;
import com.eshop.app.dto.response.*;
import com.eshop.app.security.CurrentUser;
import com.eshop.app.security.RateLimited;
import com.eshop.app.security.UserPrincipal;
import com.eshop.app.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.eshop.app.constants.ApiConstants;

/**
 * Authentication Controller - Handles user authentication operations.
 * 
 * <p>Endpoints:</p>
 * <ul>
 *   <li>POST /register - User registration</li>
 *   <li>POST /login - User login</li>
 *   <li>POST /refresh - Refresh access token</li>
 *   <li>POST /logout - User logout</li>
 *   <li>POST /logout-all - Logout from all devices</li>
 *   <li>GET /me - Get current user</li>
 *   <li>POST /forgot-password - Request password reset</li>
 *   <li>POST /reset-password - Reset password with token</li>
 *   <li>POST /change-password - Change password (authenticated)</li>
 *   <li>POST /verify-email - Verify email address</li>
 *   <li>POST /resend-verification - Resend verification email</li>
 * </ul>
 *
 * @author EShop Team
 * @version 2.0
 */
@Slf4j
@RestController
@RequestMapping(ApiConstants.Endpoints.AUTH)
@Tag(name = "Authentication", description = "User registration, login, and authentication management")
@Validated
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ==================== REGISTRATION ====================

    @PostMapping("/register")
    @RateLimited(requests = 5, period = 3600, key = "register") // 5 registrations per hour per IP
    @Operation(
        summary = "Register new user",
        description = """
            Create a new user account with role-based registration.
            
            **Required fields for all roles:**
            - username, email, password, confirmPassword, firstName, lastName, phone
            
            **Role-specific requirements:**
            - CUSTOMER: No additional fields required
            - SELLER: sellerType required
              - FARMER: aadharNumber, farmingLandArea required
              - SHOP/WHOLESALER: shopName, businessName required
            - DELIVERY_AGENT: vehicleType required
            
            **Password requirements:**
            - Minimum 8 characters
            - At least one uppercase letter
            - At least one lowercase letter
            - At least one digit
            - At least one special character
            """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "User registration details",
        required = true,
        content = @Content(
            mediaType = "application/json",
            examples = {
                @ExampleObject(
                    name = "Customer",
                    summary = "Register as Customer",
                    value = """
                        {
                          "username": "john_doe",
                          "email": "john@example.com",
                          "password": "SecurePass123!",
                          "confirmPassword": "SecurePass123!",
                          "firstName": "John",
                          "lastName": "Doe",
                          "phone": "+1-555-0123",
                          "role": "CUSTOMER"
                        }
                        """
                ),
                @ExampleObject(
                    name = "Farmer Seller",
                    summary = "Register as Farmer",
                    value = """
                        {
                          "username": "farmer_ram",
                          "email": "ram.farmer@example.com",
                          "password": "FarmerPass123!",
                          "confirmPassword": "FarmerPass123!",
                          "firstName": "Ram",
                          "lastName": "Kumar",
                          "phone": "9876543210",
                          "role": "SELLER",
                          "sellerType": "FARMER",
                          "aadharNumber": "123456789012",
                          "farmingLandArea": "5 acres"
                        }
                        """
                )
            }
        )
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "User registered successfully",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input - validation failed"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Conflict - username or email already exists"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "429",
            description = "Too many registration attempts"
        )
    })
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        
        // Secure logging - don't log sensitive data
        log.info("Registration attempt - IP: {}, Role: {}", 
                getClientIP(httpRequest), 
                request.getRole());
        
        AuthResponse response = authService.register(request, getClientIP(httpRequest));
        
        log.info("Registration successful - User ID: {}", response.getUserId());
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully. Please verify your email.", response));
    }

    // ==================== LOGIN ====================

    @PostMapping("/login")
    @RateLimited(requests = 5, period = 60, key = "login") // 5 attempts per minute per IP
    @Operation(
        summary = "User login",
        description = """
            Authenticate user with username/email and password.
            
            **Returns:**
            - Access token (expires in 15 minutes)
            - Refresh token (expires in 7 days)
            
            **Test accounts:**
            - Admin: admin / Admin@123
            - Customer: customer1 / Customer@123
            """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Login credentials",
        required = true,
        content = @Content(
            mediaType = "application/json",
            examples = {
                @ExampleObject(
                    name = "Username Login",
                    value = """
                        {
                          "usernameOrEmail": "customer1",
                          "password": "Customer@123"
                        }
                        """
                ),
                @ExampleObject(
                    name = "Email Login",
                    value = """
                        {
                          "usernameOrEmail": "customer@example.com",
                          "password": "Customer@123"
                        }
                        """
                )
            }
        )
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Login successful"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Invalid credentials"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Account locked or disabled"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "429",
            description = "Too many login attempts - account temporarily locked"
        )
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("Login attempt - IP: {}", getClientIP(httpRequest));
        
        AuthResponse response = authService.login(request, getClientIP(httpRequest));
        
        log.info("Login successful - User ID: {}", response.getUserId());
        
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    // ==================== TOKEN REFRESH ====================

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token",
        description = "Get new access token using refresh token. Old refresh token is invalidated (rotation)."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Token refreshed successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Invalid or expired refresh token"
        )
    })
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        
        log.debug("Token refresh requested");
        
        AuthResponse response = authService.refreshToken(request);
        
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    // ==================== LOGOUT ====================

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Logout",
        description = "Logout current session and invalidate refresh token",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Logged out successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request,
            @CurrentUser UserPrincipal currentUser) {
        
        log.info("Logout - User ID: {}", currentUser.getId());
        
        authService.logout(request.getRefreshToken(), currentUser.getId());
        
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Logout from all devices",
        description = "Invalidate all refresh tokens for the current user",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    public ResponseEntity<ApiResponse<Void>> logoutAll(@CurrentUser UserPrincipal currentUser) {
        
        log.info("Logout all devices - User ID: {}", currentUser.getId());
        
        authService.logoutAll(currentUser.getId());
        
        return ResponseEntity.ok(ApiResponse.success("Logged out from all devices", null));
    }

    // ==================== CURRENT USER ====================

    @GetMapping("/me")
    @PreAuthorize("permitAll()")
    @Operation(
        summary = "Get current user",
        description = "Retrieve the currently authenticated user's profile information",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User information retrieved successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing token"
        )
    })
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @CurrentUser UserPrincipal currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
        }

        UserResponse response = authService.getCurrentUser(currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== PASSWORD MANAGEMENT ====================

    @PostMapping("/forgot-password")
    @RateLimited(requests = 3, period = 3600, key = "forgot-password") // 3 requests per hour
    @Operation(
        summary = "Request password reset",
        description = "Send password reset link to user's email. Link expires in 1 hour."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Password reset email sent (if email exists)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "429",
            description = "Too many requests"
        )
    })
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        
        log.info("Password reset requested for email: {}", maskEmail(request.getEmail()));
        
        authService.forgotPassword(request.getEmail());
        
        // Always return success to prevent email enumeration
        return ResponseEntity.ok(ApiResponse.success(
            "If an account exists with this email, a password reset link has been sent.", null));
    }

    @PostMapping("/reset-password")
    @Operation(
        summary = "Reset password",
        description = "Reset password using the token received via email"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Password reset successful"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid or expired token"
        )
    })
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        
        log.info("Password reset attempt with token");
        
        authService.resetPassword(request);
        
        return ResponseEntity.ok(ApiResponse.success("Password reset successful. Please login with your new password.", null));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Change password",
        description = "Change password for authenticated user",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Password changed successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Current password is incorrect"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @CurrentUser UserPrincipal currentUser) {
        
        log.info("Password change - User ID: {}", currentUser.getId());
        
        authService.changePassword(currentUser.getId(), request);
        
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    // ==================== EMAIL VERIFICATION ====================

    @PostMapping("/verify-email")
    @Operation(
        summary = "Verify email address",
        description = "Verify user's email address using the token sent via email"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Email verified successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid or expired verification token"
        )
    })
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam @Parameter(description = "Email verification token") String token) {
        
        log.info("Email verification attempt");
        
        authService.verifyEmail(token);
        
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", null));
    }

    @PostMapping("/resend-verification")
    @PreAuthorize("isAuthenticated()")
    @RateLimited(requests = 3, period = 3600, key = "resend-verification")
    @Operation(
        summary = "Resend verification email",
        description = "Resend email verification link",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(
            @CurrentUser UserPrincipal currentUser) {
        
        log.info("Resend verification - User ID: {}", currentUser.getId());
        
        authService.resendVerificationEmail(currentUser.getId());
        
        return ResponseEntity.ok(ApiResponse.success("Verification email sent", null));
    }

    // ==================== TWO-FACTOR AUTHENTICATION ====================

    @PostMapping("/2fa/enable")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Enable 2FA",
        description = "Enable two-factor authentication for the account",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    public ResponseEntity<ApiResponse<TwoFactorSetupResponse>> enableTwoFactor(
            @CurrentUser UserPrincipal currentUser) {
        
        log.info("2FA enable - User ID: {}", currentUser.getId());
        
        TwoFactorSetupResponse response = authService.enableTwoFactor(currentUser.getId());
        
        return ResponseEntity.ok(ApiResponse.success("Scan QR code with authenticator app", response));
    }

    @PostMapping("/2fa/verify")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Verify 2FA setup",
        description = "Verify 2FA setup with code from authenticator app",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    public ResponseEntity<ApiResponse<Void>> verifyTwoFactor(
            @Valid @RequestBody TwoFactorVerifyRequest request,
            @CurrentUser UserPrincipal currentUser) {
        
        authService.verifyTwoFactorSetup(currentUser.getId(), request.getCode());
        
        return ResponseEntity.ok(ApiResponse.success("Two-factor authentication enabled", null));
    }

    @PostMapping("/2fa/disable")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Disable 2FA",
        description = "Disable two-factor authentication",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    public ResponseEntity<ApiResponse<Void>> disableTwoFactor(
            @Valid @RequestBody TwoFactorDisableRequest request,
            @CurrentUser UserPrincipal currentUser) {
        
        log.info("2FA disable - User ID: {}", currentUser.getId());
        
        authService.disableTwoFactor(currentUser.getId(), request.getPassword(), request.getCode());
        
        return ResponseEntity.ok(ApiResponse.success("Two-factor authentication disabled", null));
    }

    @PostMapping("/2fa/validate")
    @Operation(
        summary = "Validate 2FA code",
        description = "Validate 2FA code during login"
    )
    public ResponseEntity<ApiResponse<AuthResponse>> validateTwoFactor(
            @Valid @RequestBody TwoFactorLoginRequest request) {
        
        AuthResponse response = authService.validateTwoFactorLogin(request);
        
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    // ==================== SESSION MANAGEMENT ====================

    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get active sessions",
        description = "List all active sessions/devices for the current user",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    public ResponseEntity<ApiResponse<java.util.List<SessionResponse>>> getActiveSessions(
            @CurrentUser UserPrincipal currentUser) {
        
        java.util.List<SessionResponse> sessions = authService.getActiveSessions(currentUser.getId());
        
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Revoke session",
        description = "Revoke a specific session/device",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @PathVariable String sessionId,
            @CurrentUser UserPrincipal currentUser) {
        
        log.info("Revoke session {} - User ID: {}", sessionId, currentUser.getId());
        
        authService.revokeSession(currentUser.getId(), sessionId);
        
        return ResponseEntity.ok(ApiResponse.success("Session revoked", null));
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Extract client IP address from request.
     * Handles proxy headers (X-Forwarded-For).
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        return request.getRemoteAddr();
    }

    /**
     * Mask email for logging (security).
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 2) {
            return "*".repeat(localPart.length()) + "@" + domain;
        }
        return localPart.charAt(0) + "*".repeat(localPart.length() - 2) + 
               localPart.charAt(localPart.length() - 1) + "@" + domain;
    }

    /**
     * Mask username for logging (security).
     */
    @SuppressWarnings("unused")
    private String maskUsername(String username) {
        if (username == null || username.length() <= 2) {
            return "***";
        }
        return username.charAt(0) + "*".repeat(username.length() - 2) + 
               username.charAt(username.length() - 1);
    }
}