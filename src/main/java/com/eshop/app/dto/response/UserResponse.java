package com.eshop.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String role;
    private Boolean active;
    private Boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String sellerType;
    private StoreInfo shop;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreInfo {
        private Long id;
        private String storeName;
        private String sellerType;
    }
}
