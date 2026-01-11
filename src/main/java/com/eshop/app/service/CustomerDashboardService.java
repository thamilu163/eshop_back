package com.eshop.app.service;

import com.eshop.app.dto.response.CustomerDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CustomerDashboardService {

        private final UserService userService;
        private final com.eshop.app.service.OrderService orderService;
        private final com.eshop.app.service.ProductService productService;

    public CustomerDashboardResponse getDashboard(Long customerId) {
        CustomerDashboardResponse.AccountInfo account = CustomerDashboardResponse.AccountInfo.builder()
                .customerName(null)
                .email(null)
                .memberSince(userService.getMemberSinceByUserId(customerId) != null ? userService.getMemberSinceByUserId(customerId).toString() : null)
                .accountStatus("Active")
                .emailVerified(null)
                .totalOrders(orderService.getOrderCountByCustomerId(customerId))
                .build();

        CustomerDashboardResponse.CartInfo cart = CustomerDashboardResponse.CartInfo.builder()
                .itemCount(0L)
                .totalValue(java.math.BigDecimal.ZERO)
                .build();

        CustomerDashboardResponse.WishlistInfo wishlist = CustomerDashboardResponse.WishlistInfo.builder()
                .itemCount(0L)
                .recentlyAdded(java.util.List.of())
                .build();

        CustomerDashboardResponse.OrderStats stats = CustomerDashboardResponse.OrderStats.builder()
                .totalSpent(orderService.getTotalSpentByCustomerId(customerId))
                .averageOrderValue(orderService.getAverageOrderValueByCustomerId(customerId))
                .favoriteCategory(productService.getFavoriteCategoryByCustomerId(customerId).orElse(null))
                .build();

        return CustomerDashboardResponse.builder()
                .accountInfo(account)
                .recentOrders(orderService.getRecentOrdersByCustomerId(customerId, 5))
                .cartInfo(cart)
                .wishlistInfo(wishlist)
                .recommendations(java.util.List.of())
                .orderStats(stats)
                .role("CUSTOMER")
                .timestamp(Instant.now())
                .build();
    }
    
    /**
     * Find customer ID by username (for Keycloak integration)
     */
        public Long findCustomerIdByUsername(String username, String email, String firstName, String lastName, Boolean emailVerified) {
                java.util.Optional<Long> idOpt = userService.findUserIdByUsernameOrEmail(username);
                if (idOpt.isPresent()) return idOpt.get();

                // Try by email if username lookup failed
                if (email != null && !email.isBlank()) {
                        idOpt = userService.findUserIdByUsernameOrEmail(email);
                        if (idOpt.isPresent()) return idOpt.get();
                }

                // Create local user from claims for first-time Keycloak users (dev-friendly behavior)
                return userService.createUserFromClaims(username, email, firstName, lastName, emailVerified);
        }
}
