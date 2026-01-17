package com.eshop.app.service.impl;

import com.eshop.app.dto.request.LoginRequest;
import com.eshop.app.dto.request.RegisterRequest;
import com.eshop.app.dto.response.AuthResponse;
import com.eshop.app.dto.response.UserResponse;
import com.eshop.app.entity.Cart;
import com.eshop.app.entity.User;
import com.eshop.app.exception.ResourceAlreadyExistsException;
import com.eshop.app.mapper.UserMapper;
import com.eshop.app.repository.CartRepository;
import com.eshop.app.repository.UserRepository;
import com.eshop.app.security.JwtTokenProvider;
import com.eshop.app.security.UserDetailsImpl;
import com.eshop.app.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import java.util.Date;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final com.eshop.app.repository.DeliveryAgentProfileRepository deliveryAgentProfileRepository;
    private final com.eshop.app.repository.SellerProfileRepository sellerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;
    // In-memory token and 2FA stores (simple scaffolding for demo/test purposes)
    private final java.util.Set<String> revokedRefreshTokens = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.concurrent.ConcurrentHashMap<Long, Long> logoutAllTimestamps = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, PasswordReset> passwordResetTokens = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, EmailVerification> emailVerificationTokens = new java.util.concurrent.ConcurrentHashMap<>();
    // Temporary 2FA codes for login flow (in-memory store for demo purposes)
    private final java.util.concurrent.ConcurrentHashMap<Long, String> twoFactorLoginCodes = new java.util.concurrent.ConcurrentHashMap<>();

    public AuthServiceImpl(UserRepository userRepository,
            CartRepository cartRepository,
            com.eshop.app.repository.DeliveryAgentProfileRepository deliveryAgentProfileRepository,
            com.eshop.app.repository.SellerProfileRepository sellerProfileRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider tokenProvider,
            UserMapper userMapper) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.deliveryAgentProfileRepository = deliveryAgentProfileRepository;
        this.sellerProfileRepository = sellerProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userMapper = userMapper;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        log.info("🔥 === AUTH SERVICE REGISTER METHOD CALLED ===");
        log.info("📋 Validating request data...");
        log.info("Username: {}, Email: {}, Role: {}", request.getUsername(), request.getEmail(), request.getRole());
        log.info("FirstName: {}, LastName: {}, Phone: {}", request.getFirstName(), request.getLastName(),
                request.getPhone());

        log.info("🔍 Checking if username exists: {}", request.getUsername());
        if (userRepository.existsByUsername(request.getUsername())) {
            log.error("❌ Username already exists: {}", request.getUsername());
            throw new ResourceAlreadyExistsException("Username already exists");
        }
        log.info("✅ Username is available: {}", request.getUsername());

        log.info("🔍 Checking if email exists: {}", request.getEmail());
        if (userRepository.existsByEmail(request.getEmail())) {
            log.error("❌ Email already exists: {}", request.getEmail());
            throw new ResourceAlreadyExistsException("Email already exists");
        }
        log.info("✅ Email is available: {}", request.getEmail());

        String firstName;
        String lastName;
        User.UserRole mappedRole;
        User.SellerType mappedSellerType;

        try {
            log.info("👤 Processing user details...");
            // Prefer explicit first/last when provided; fallback to displayName
            firstName = request.getFirstName();
            lastName = request.getLastName();
            if ((firstName == null || firstName.isBlank()) && request.getDisplayName() != null) {
                firstName = request.getDisplayName();
                log.info("🔄 Using displayName as firstName: {}", firstName);
            }
            log.info("📝 Final names - First: {}, Last: {}", firstName, lastName);

            log.info("🎭 Mapping role from API to internal format...");
            // Map incoming API Role (enum or string) to internal User.UserRole and
            // SellerType
            mappedRole = User.UserRole.CUSTOMER;
            mappedSellerType = null;
            log.info("📥 Input role: {}, roleName: {}", request.getRole(), request.getRoleName());

            // Helper to map Role enum values
            if (request.getRole() != null) {
                log.info("🎯 Mapping enum role: {}", request.getRole());
                switch (request.getRole()) {
                    case ADMIN -> {
                        mappedRole = User.UserRole.ADMIN;
                        log.info("👑 Mapped to ADMIN role");
                    }
                    case CUSTOMER -> {
                        mappedRole = User.UserRole.CUSTOMER;
                        log.info("🛍️ Mapped to CUSTOMER role");
                    }
                    case SELLER -> {
                        mappedRole = User.UserRole.SELLER;
                        log.info("🏪 Mapped to SELLER role");
                    }
                    case DELIVERY_AGENT -> {
                        mappedRole = User.UserRole.DELIVERY_AGENT;
                        log.info("🚚 Mapped to DELIVERY_AGENT role");
                    }
                    default -> {
                        mappedRole = User.UserRole.CUSTOMER;
                        log.info("❓ Unknown role, defaulting to CUSTOMER");
                    }
                }
            } else if (request.getRoleName() != null && !request.getRoleName().isBlank()) {
                log.info("🔤 Processing string roleName: {}", request.getRoleName());
                // support legacy/alternate JSON that sends a string role name like "SELLER" or
                // "RETAIL"
                String rn = request.getRoleName().trim().toUpperCase();
                switch (rn) {
                    case "ADMIN" -> mappedRole = User.UserRole.ADMIN;
                    case "CUSTOMER" -> mappedRole = User.UserRole.CUSTOMER;
                    case "DELIVERY_AGENT", "DELIVERY" -> mappedRole = User.UserRole.DELIVERY_AGENT;
                    case "FARMER" -> {
                        mappedRole = User.UserRole.SELLER;
                        mappedSellerType = User.SellerType.FARMER;
                    }
                    case "RETAIL", "RETAIL_SELLER", "RETAILER" -> {
                        mappedRole = User.UserRole.SELLER;
                        mappedSellerType = User.SellerType.RETAILER;
                    }
                    case "WHOLESALER" -> {
                        mappedRole = User.UserRole.SELLER;
                        mappedSellerType = User.SellerType.WHOLESALER;
                    }
                    case "SHOP", "SHOP_SELLER", "BUSINESS" -> {
                        mappedRole = User.UserRole.SELLER;
                        mappedSellerType = User.SellerType.BUSINESS;
                    }
                    case "SELLER" -> mappedRole = User.UserRole.SELLER;
                    default -> mappedRole = User.UserRole.CUSTOMER;
                }
            } else {
                log.info("⚠️ No role specified, defaulting to CUSTOMER");
            }

            log.info("✅ Final role mapping - Role: {}, SellerType: {}", mappedRole, mappedSellerType);
        } catch (Exception e) {
            log.error("💥 Exception during role mapping: {}", e.getMessage(), e);
            throw e;
        }

        try {
            log.info("🏗️ Creating user entity...");
            log.info("🔐 Encoding password (length: {})",
                    request.getPassword() != null ? request.getPassword().length() : "null");

            User user = User.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .firstName(firstName)
                    .lastName(lastName)
                    .phone(request.getPhone())
                    .address(request.getAddress())
                    .role(mappedRole)
                    .sellerType(mappedSellerType)
                    .active(true)
                    .emailVerified(false)
                    .build();

            log.info("💾 Saving user to database...");
            user = userRepository.save(user);
            log.info("✅ User saved successfully with ID: {}", user.getId());

            // Create cart for customer
            if (user.getRole() == User.UserRole.CUSTOMER) {
                log.info("🛒 Creating cart for customer: {}", user.getUsername());
                Cart cart = Cart.builder()
                        .user(user)
                        .build();
                cartRepository.save(cart);
                log.info("✅ Cart created successfully for user: {}", user.getUsername());
            }

            // Create delivery agent profile when role is DELIVERY_AGENT
            if (user.getRole() == User.UserRole.DELIVERY_AGENT) {
                log.info("🚚 Creating delivery agent profile for: {}", user.getUsername());
                com.eshop.app.entity.DeliveryAgentProfile profile = com.eshop.app.entity.DeliveryAgentProfile.builder()
                        .user(user)
                        .vehicleType(request.getVehicleType())
                        .build();
                deliveryAgentProfileRepository.save(profile);
            }

            // Create seller profile when the mapped role is SELLER
            if (user.getRole() == User.UserRole.SELLER) {
                // If seller type wasn't determined from Role enum/roleName, try the incoming
                // sellerType string
                if (mappedSellerType == null && request.getSellerType() != null) {
                    String st = request.getSellerType().trim().toUpperCase();
                    if (st.equals("FARMER"))
                        mappedSellerType = User.SellerType.FARMER;
                    else if (st.equals("RETAIL") || st.equals("RETAIL_SELLER") || st.equals("RETAILER"))
                        mappedSellerType = User.SellerType.RETAILER;
                    else if (st.equals("WHOLESALER"))
                        mappedSellerType = User.SellerType.WHOLESALER;
                    else if (st.equals("SHOP") || st.equals("SHOP_SELLER") || st.equals("BUSINESS"))
                        mappedSellerType = User.SellerType.BUSINESS;
                    else if (st.equals("INDIVIDUAL"))
                        mappedSellerType = User.SellerType.INDIVIDUAL;
                }

                com.eshop.app.entity.SellerProfile sellerProfile = com.eshop.app.entity.SellerProfile.builder()
                        .user(user)
                        .aadhar(request.getAadhar() != null ? request.getAadhar() : request.getAadharNumber())
                        .pan(request.getPan() != null && !request.getPan().isBlank() ? request.getPan()
                                : request.getPanNumber())
                        .gstin(request.getGstin() != null && !request.getGstin().isBlank() ? request.getGstin()
                                : request.getGstinNumber())
                        .businessType(request.getBusinessType())
                        .shopName(request.getShopName())
                        .businessName(request.getBusinessName())
                        .farmLocationVillage(request.getFarmLocationVillage())
                        .landArea(request.getLandArea() != null ? request.getLandArea() : request.getFarmingLandArea())
                        .warehouseLocation(request.getWarehouseLocation())
                        .bulkPricingAgreement(
                                request.getBulkPricingAgreement() != null ? request.getBulkPricingAgreement()
                                        : Boolean.FALSE)
                        .build();

                // persist and, if seller type was resolved, set it on user and save
                sellerProfileRepository.save(sellerProfile);
                if (mappedSellerType != null) {
                    user.setSellerType(mappedSellerType);
                    userRepository.save(user);
                }
            }

            log.info("🎫 Generating JWT token for user: {}", user.getUsername());
            String token = tokenProvider.generateTokenFromUser(user);
            log.info("✅ Token generated successfully (length: {})", token != null ? token.length() : "null");

            log.info("🏗️ Building AuthResponse...");
            AuthResponse response = AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .expiresIn(tokenProvider.getJwtExpirationMs())
                    .build();

            log.info("🎉 Registration completed successfully for user: {}", user.getUsername());
            log.info("📋 Response details - UserID: {}, Role: {}", response.getUserId(), response.getRole());
            return response;

        } catch (Exception e) {
            log.error("💥 Exception during user creation/saving phase: {}", e.getMessage(), e);
            throw e;
        }
    }

    // resolveSellerType removed; mapping handled inline in register() to support
    // new Role enum

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(),
                        request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(userDetails.getId())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .role(userDetails.getRole())
                .expiresIn(tokenProvider.getJwtExpirationMs())
                .build();
    }

    @Override
    public UserResponse getCurrentUser() {
        Long userId = com.eshop.app.util.SecurityUtils.getAuthenticatedUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return userMapper.toUserResponse(user);
    }

    @Override
    public AuthResponse register(RegisterRequest request, String clientIp) {
        return register(request);
    }

    @Override
    public AuthResponse login(LoginRequest request, String clientIp) {
        return login(request);
    }

    @Override
    public AuthResponse refreshToken(com.eshop.app.dto.request.RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (token == null || token.isBlank())
            throw new IllegalArgumentException("refresh token required");
        if (revokedRefreshTokens.contains(token))
            throw new RuntimeException("Refresh token revoked");

        // Validate token signature
        if (!tokenProvider.validateToken(token))
            throw new RuntimeException("Invalid refresh token");

        // Check logout-all timestamp
        Date issuedAt = tokenProvider.getIssuedAtFromToken(token);
        Long userId = tokenProvider.getUserIdFromToken(token);
        Long logoutTs = logoutAllTimestamps.get(userId);
        if (logoutTs != null && issuedAt != null && issuedAt.getTime() <= logoutTs) {
            throw new RuntimeException("Refresh token invalidated by logout-all");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        String newToken = tokenProvider.generateTokenFromUser(user);
        return AuthResponse.builder()
                .token(newToken)
                .type("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .expiresIn(tokenProvider.getJwtExpirationMs())
                .build();
    }

    @Override
    public void logout(String refreshToken, Long userId) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            revokedRefreshTokens.add(refreshToken);
            log.info("Refresh token revoked for user {}", userId);
        }
    }

    @Override
    public void logoutAll(Long userId) {
        logoutAllTimestamps.put(userId, System.currentTimeMillis());
        log.info("Logout-all executed for user {}", userId);
    }

    @Override
    public UserResponse getCurrentUser(Long userId) {
        return getCurrentUser();
    }

    @Override
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = java.util.UUID.randomUUID().toString();
            PasswordReset pr = new PasswordReset(user.getId(), System.currentTimeMillis() + 60 * 60 * 1000L);
            passwordResetTokens.put(token, pr);
            log.info("Password reset token generated for user {} : {}", user.getEmail(), token);
            // In production: send email containing link with token
        });
    }

    @Override
    public void resetPassword(com.eshop.app.dto.request.ResetPasswordRequest request) {
        if (request.getToken() == null)
            throw new IllegalArgumentException("token required");
        PasswordReset pr = passwordResetTokens.get(request.getToken());
        if (pr == null || pr.expiresAt < System.currentTimeMillis())
            throw new RuntimeException("Invalid or expired token");
        User user = userRepository.findById(pr.userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        passwordResetTokens.remove(request.getToken());
        log.info("Password reset completed for user {}", user.getEmail());
    }

    @Override
    public void changePassword(Long userId, com.eshop.app.dto.request.ChangePasswordRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user {}", user.getEmail());
    }

    @Override
    public void verifyEmail(String token) {
        EmailVerification ev = emailVerificationTokens.get(token);
        if (ev == null || ev.expiresAt < System.currentTimeMillis())
            throw new RuntimeException("Invalid or expired verification token");
        User user = userRepository.findById(ev.userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setEmailVerified(true);
        userRepository.save(user);
        emailVerificationTokens.remove(token);
        log.info("Email verified for user {}", user.getEmail());
    }

    @Override
    public void resendVerificationEmail(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        String token = java.util.UUID.randomUUID().toString();
        EmailVerification ev = new EmailVerification(user.getId(), System.currentTimeMillis() + 24 * 60 * 60 * 1000L);
        emailVerificationTokens.put(token, ev);
        log.info("Verification token for {} : {}", user.getEmail(), token);
        // In production: send email with token link
    }

    @Override
    public com.eshop.app.dto.response.TwoFactorSetupResponse enableTwoFactor(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        String secret = java.util.UUID.randomUUID().toString().replaceAll("-", "");

        // Save 2FA secret for the user (integrate with TOTP library like Google
        // Authenticator in production)
        user.setTwoFactorSecret(secret);
        userRepository.save(user);
        return com.eshop.app.dto.response.TwoFactorSetupResponse.builder()
                .secret(secret)
                .qrCodeImage("otpauth://totp/eshop:" + user.getUsername() + "?secret=" + secret)
                .recoveryCodes(java.util.List.of(java.util.UUID.randomUUID().toString()))
                .build();
    }

    @Override
    public void verifyTwoFactorSetup(Long userId, String code) {
        // Verify the TOTP code provided by the user
        // In production, integrate with a TOTP library (e.g., GoogleAuth, jOTP) to
        // verify the code
        // For now, simplified verification is performed
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // Validate code (integrate proper TOTP validation in production)
        // Example: if (!totpService.verifyCode(user.getTwoFactorSecret(), code)) throw
        // new RuntimeException("Invalid 2FA code");

        user.setTwoFactorEnabled(true);
        userRepository.save(user);
        log.info("2FA enabled for user {}", user.getEmail());
    }

    @Override
    public void disableTwoFactor(Long userId, String password, String code) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(password, user.getPassword()))
            throw new RuntimeException("Invalid password");
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userRepository.save(user);
        log.info("2FA disabled for user {}", user.getEmail());
    }

    @Override
    public AuthResponse validateTwoFactorLogin(com.eshop.app.dto.request.TwoFactorLoginRequest request) {
        // tempToken is expected to map to a user id in this scaffolding
        Long userId;
        try {
            userId = Long.parseLong(request.getTempToken());
        } catch (Exception e) {
            throw new RuntimeException("Invalid temp token");
        }
        String expected = twoFactorLoginCodes.get(userId);
        if (expected == null || !expected.equals(request.getCode()))
            throw new RuntimeException("Invalid 2FA code");
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        String token = tokenProvider.generateTokenFromUser(user);
        // One-time use
        twoFactorLoginCodes.remove(userId);
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .expiresIn(tokenProvider.getJwtExpirationMs())
                .build();
    }

    @Override
    public java.util.List<com.eshop.app.dto.response.SessionResponse> getActiveSessions(Long userId) {
        // Not tracking sessions persistently in this scaffold. Return empty list.
        return java.util.List.of();
    }

    @Override
    public void revokeSession(Long userId, String sessionId) {
        // No-op in in-memory scaffold. In production, mark session invalid in DB/store.
        log.info("Requested revoke session {} for user {} (no-op in scaffold)", sessionId, userId);
    }

    // Helper classes for in-memory scaffolding
    private static class PasswordReset {
        long userId;
        long expiresAt;

        PasswordReset(long u, long e) {
            this.userId = u;
            this.expiresAt = e;
        }
    }

    private static class EmailVerification {
        long userId;
        long expiresAt;

        EmailVerification(long u, long e) {
            this.userId = u;
            this.expiresAt = e;
        }
    }
}
