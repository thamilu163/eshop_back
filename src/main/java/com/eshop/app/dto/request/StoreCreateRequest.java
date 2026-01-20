package com.eshop.app.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreCreateRequest {

    @NotBlank(message = "Store name is required")
    @Size(max = 200, message = "Store name must not exceed 200 characters")
    private String storeName;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;

    @Size(max = 500, message = "Logo URL must not exceed 500 characters")
    private String logoUrl;

    // Auto-populated from JWT token in controller
    private Long sellerId;


}
