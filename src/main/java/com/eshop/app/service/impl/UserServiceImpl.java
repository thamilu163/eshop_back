package com.eshop.app.service.impl;

import com.eshop.app.dto.request.UserUpdateRequest;
import com.eshop.app.dto.request.UserSelfUpdateRequest;
import com.eshop.app.dto.request.PasswordChangeRequest;
import com.eshop.app.dto.request.ChangePasswordRequest;
import com.eshop.app.dto.response.BulkOperationResult;
import com.eshop.app.enums.ExportFormat;
import com.eshop.app.service.AuthService;
import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.dto.response.UserResponse;
import com.eshop.app.entity.User;
import com.eshop.app.exception.ResourceNotFoundException;
import com.eshop.app.mapper.UserMapper;
import com.eshop.app.repository.UserRepository;
import com.eshop.app.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AuthService authService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    
    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, AuthService authService,
                           org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("User not found with id: {}", id);
                return new ResourceNotFoundException("User not found with id: " + id);
            });
        return userMapper.toUserResponse(user);
    }
    
    @Override
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        
        user = userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse updateSelf(Long id, UserSelfUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAddress() != null) user.setAddress(request.getAddress());

        user = userRepository.save(user);
        return userMapper.toUserResponse(user);
    }
    
    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);
        return PageResponse.of(userPage, userMapper::toUserResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsersByRole(String role, Pageable pageable) {
        User.UserRole userRole = User.UserRole.valueOf(role.toUpperCase());
        Page<User> userPage = userRepository.findByRole(userRole, pageable);
        return PageResponse.of(userPage, userMapper::toUserResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> searchUsers(String keyword, Pageable pageable) {
        Page<User> userPage = userRepository.searchUsers(keyword, pageable);
        return PageResponse.of(userPage, userMapper::toUserResponse);
    }

    @Override
    public PageResponse<UserResponse> getUsersByActiveStatus(Boolean active, Pageable pageable) {
        Page<User> userPage = userRepository.findByActive(active, pageable);
        return PageResponse.of(userPage, userMapper::toUserResponse);
    }
    
    @Override
    public UserResponse activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setActive(true);
        user = userRepository.save(user);
        return userMapper.toUserResponse(user);
    }
    
    @Override
    public UserResponse deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setActive(false);
        user = userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse changeRole(Long id, com.eshop.app.enums.UserRole newRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        // Map API enum to entity enum
        try {
            User.UserRole entityRole = User.UserRole.valueOf(newRole.name());
            user.setRole(entityRole);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Invalid role: " + newRole);
        }
        user = userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    @Override
    public void changePassword(Long id, PasswordChangeRequest request) {
        // Delegate to AuthService which contains password handling logic
        ChangePasswordRequest cr = new ChangePasswordRequest();
        cr.setCurrentPassword(request.getCurrentPassword());
        cr.setNewPassword(request.getNewPassword());
        cr.setConfirmPassword(request.getNewPassword());
        authService.changePassword(id, cr);
    }

    @Override
    public void hardDeleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public void softDeleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    public BulkOperationResult bulkActivate(java.util.List<Long> userIds) {
        java.util.List<User> users = userRepository.findAllById(userIds);
        java.util.Set<Long> found = new java.util.HashSet<>();
        for (User u : users) found.add(u.getId());
        java.util.List<Long> failed = new java.util.ArrayList<>();
        for (Long id : userIds) if (!found.contains(id)) failed.add(id);
        for (User u : users) u.setActive(true);
        userRepository.saveAll(users);
        return BulkOperationResult.builder()
                .totalProcessed(userIds.size())
                .successCount(users.size())
                .failedCount(failed.size())
                .failedIds(failed)
                .build();
    }

    @Override
    public BulkOperationResult bulkDeactivate(java.util.List<Long> userIds) {
        java.util.List<User> users = userRepository.findAllById(userIds);
        java.util.Set<Long> found = new java.util.HashSet<>();
        for (User u : users) found.add(u.getId());
        java.util.List<Long> failed = new java.util.ArrayList<>();
        for (Long id : userIds) if (!found.contains(id)) failed.add(id);
        for (User u : users) u.setActive(false);
        userRepository.saveAll(users);
        return BulkOperationResult.builder()
                .totalProcessed(userIds.size())
                .successCount(users.size())
                .failedCount(failed.size())
                .failedIds(failed)
                .build();
    }

    @Override
    public byte[] exportUsers(ExportFormat format, com.eshop.app.enums.UserRole role, Boolean active) {
        java.util.List<User> all = userRepository.findAll();
        java.util.List<User> filtered = new java.util.ArrayList<>();
        for (User u : all) {
            if (role != null) {
                try {
                    if (!u.getRole().name().equals(role.name())) continue;
                } catch (Exception ignored) { continue; }
            }
            if (active != null && !active.equals(u.getActive())) continue;
            filtered.add(u);
        }

        try {
            if (format == ExportFormat.EXCEL) {
                org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("Users");
                org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
                String[] cols = new String[]{"Id","Username","Email","FirstName","LastName","Role","Active"};
                for (int i=0;i<cols.length;i++) header.createCell(i).setCellValue(cols[i]);
                int r = 1;
                for (User u : filtered) {
                    org.apache.poi.ss.usermodel.Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(u.getId());
                    row.createCell(1).setCellValue(u.getUsername());
                    row.createCell(2).setCellValue(u.getEmail());
                    row.createCell(3).setCellValue(u.getFirstName() == null ? "" : u.getFirstName());
                    row.createCell(4).setCellValue(u.getLastName() == null ? "" : u.getLastName());
                    row.createCell(5).setCellValue(u.getRole() == null ? "" : u.getRole().name());
                    row.createCell(6).setCellValue(Boolean.TRUE.equals(u.getActive()) ? "true" : "false");
                }
                try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                    wb.write(out);
                    wb.close();
                    return out.toByteArray();
                }
            } else {
                // CSV
                StringBuilder sb = new StringBuilder();
                sb.append("Id,Username,Email,FirstName,LastName,Role,Active\n");
                for (User u : filtered) {
                    sb.append(u.getId()).append(',')
                      .append('"').append(u.getUsername()).append('"').append(',')
                      .append('"').append(u.getEmail()).append('"').append(',')
                      .append('"').append(u.getFirstName() == null ? "" : u.getFirstName()).append('"').append(',')
                      .append('"').append(u.getLastName() == null ? "" : u.getLastName()).append('"').append(',')
                      .append(u.getRole() == null ? "" : u.getRole().name()).append(',')
                      .append(Boolean.TRUE.equals(u.getActive()) ? "true" : "false").append('\n');
                }
                return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to export users", e);
        }
    }
    
    // Dashboard Analytics Methods Implementation
    @Override
    @Transactional(readOnly = true)
    public long getTotalUserCount() {
        return userRepository.count();
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getCustomerCount() {
        return userRepository.countByRole(User.UserRole.CUSTOMER);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getSellerCount() {
        return userRepository.countByRole(User.UserRole.SELLER);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getDeliveryAgentCount() {
        return userRepository.countByRole(User.UserRole.DELIVERY_AGENT);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getActiveUserCount() {
        return userRepository.countByActiveTrue();
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getNewUsersThisMonth() {
        java.time.LocalDateTime startOfMonth = java.time.LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        return userRepository.countByCreatedAtAfter(startOfMonth);
    }
    
    @Override
    @Transactional(readOnly = true)
    public java.time.LocalDate getMemberSinceByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate() : java.time.LocalDate.now();
    }
    
    @Override
    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> getUserGrowthData() {
        return userRepository.getUserGrowthData();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<Long> findUserIdByUsernameOrEmail(String usernameOrEmail) {
        if (usernameOrEmail == null) return java.util.Optional.empty();
        java.util.Optional<User> byUsername = userRepository.findByUsername(usernameOrEmail);
        if (byUsername.isPresent()) return java.util.Optional.of(byUsername.get().getId());
        java.util.Optional<User> byEmail = userRepository.findByEmail(usernameOrEmail);
        return byEmail.map(User::getId);
    }

    @Override
    @Transactional
    public Long createUserFromClaims(String username, String email, String firstName, String lastName, Boolean emailVerified) {
        // Defensive checks
        if ((username == null || username.isBlank()) && (email == null || email.isBlank())) {
            throw new IllegalArgumentException("Either username or email must be provided to create user");
        }

        // Prefer username if available, otherwise derive from email
        String finalUsername = username;
        if (finalUsername == null || finalUsername.isBlank()) {
            finalUsername = email.split("@")[0];
        }

        // Avoid collision: append numeric suffix if username exists
        String candidate = finalUsername;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = finalUsername + suffix++;
        }
        finalUsername = candidate;

        String finalEmail = email != null ? email : (finalUsername + "@local.dev");

        // Random password (won't be used for OIDC login) - encoded
        String rawPassword = java.util.UUID.randomUUID().toString();
        String encoded = passwordEncoder.encode(rawPassword);

        User user = User.builder()
                .username(finalUsername)
                .email(finalEmail)
                .password(encoded)
                .firstName(firstName)
                .lastName(lastName)
                .role(User.UserRole.CUSTOMER)
                .emailVerified(Boolean.TRUE.equals(emailVerified))
                .active(true)
                .build();

        user = userRepository.save(user);

        return user.getId();
    }
}
