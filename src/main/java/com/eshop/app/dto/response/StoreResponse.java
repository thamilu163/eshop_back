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
public class StoreResponse {
    
    private Long id;
    private String storeName;
    private String description;
    private String address;
    private String phone;
    private String email;
    private String logoUrl;
    private String domain;
    private Boolean active;
    private Double rating;
    private Long sellerId;
    private String sellerUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
