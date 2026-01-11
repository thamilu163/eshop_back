package com.eshop.app.service;

import com.eshop.app.dto.request.UserUpdateRequest;
import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;
import com.eshop.app.dto.request.UserSelfUpdateRequest;
import com.eshop.app.dto.request.PasswordChangeRequest;
import com.eshop.app.dto.response.BulkOperationResult;
import com.eshop.app.enums.ExportFormat;
import com.eshop.app.enums.UserRole;

public interface UserService {
    UserResponse getUserById(Long id);
    UserResponse updateUser(Long id, UserUpdateRequest request);
    UserResponse updateSelf(Long id, UserSelfUpdateRequest request);
    void deleteUser(Long id);
    PageResponse<UserResponse> getAllUsers(Pageable pageable);
    PageResponse<UserResponse> getUsersByRole(String role, Pageable pageable);
    PageResponse<UserResponse> searchUsers(String keyword, Pageable pageable);
    PageResponse<UserResponse> getUsersByActiveStatus(Boolean active, Pageable pageable);
    UserResponse activateUser(Long id);
    UserResponse deactivateUser(Long id);
    UserResponse changeRole(Long id, UserRole newRole);
    void changePassword(Long id, PasswordChangeRequest request);
    void hardDeleteUser(Long id);
    void softDeleteUser(Long id);
    BulkOperationResult bulkActivate(java.util.List<Long> userIds);
    BulkOperationResult bulkDeactivate(java.util.List<Long> userIds);
    byte[] exportUsers(ExportFormat format, UserRole role, Boolean active);
    
    // Dashboard Analytics Methods
    long getTotalUserCount();
    long getCustomerCount();
    long getSellerCount();
    long getDeliveryAgentCount();
    long getActiveUserCount();
    long getNewUsersThisMonth();
    java.time.LocalDate getMemberSinceByUserId(Long userId);
    java.util.List<java.util.Map<String, Object>> getUserGrowthData();

    // Lookup helpers
    java.util.Optional<Long> findUserIdByUsernameOrEmail(String usernameOrEmail);

    /**
     * Create a local user from identity provider claims when no local account exists.
     * Returns the created user's id.
     */
    Long createUserFromClaims(String username, String email, String firstName, String lastName, Boolean emailVerified);
}