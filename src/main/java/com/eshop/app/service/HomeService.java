package com.eshop.app.service;

import com.eshop.app.dto.response.HomeResponse;
import com.eshop.app.entity.Order;
import com.eshop.app.entity.User;
import com.eshop.app.repository.OrderRepository;
import com.eshop.app.repository.ProductRepository;
import com.eshop.app.repository.ShopRepository;
import com.eshop.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomeService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public HomeResponse getHomePageData(Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        return getHomePageDataForUser(user);
    }

    private User getUserFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || 
            !(authentication.getPrincipal() instanceof UserDetails)) {
            return null;
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();
        
        return userRepository.findByUsername(username).orElse(null);
    }

    @Transactional(readOnly = true)
    public HomeResponse getHomePageDataForUser(User user) {
        if (user == null) {
            return HomeResponse.forGuest();
        }

        User.UserRole role = user.getRole();
        String userName = user.getFirstName() + " " + user.getLastName();
        
        log.info("Generating home page data for user: {} with role: {}", userName, role);

        return switch (role) {
            case ADMIN -> HomeResponse.forAdmin(userName, getAdminDashboardData(user));
            case SELLER -> HomeResponse.forSeller(userName, getSellerDashboardData(user));
            case CUSTOMER -> HomeResponse.forCustomer(userName, getCustomerDashboardData(user));
            case DELIVERY_AGENT -> HomeResponse.forDeliveryAgent(userName, getDeliveryAgentDashboardData(user));
            default -> HomeResponse.forGuest();
        };
    }

    private Map<String, Object> getAdminDashboardData(User user) {
        long totalUsers = userRepository != null ? userRepository.count() : 0L;
        long totalProducts = productRepository != null ? productRepository.count() : 0L;
        long totalShops = shopRepository != null ? shopRepository.count() : 0L;
        long totalOrders = orderRepository != null ? orderRepository.count() : 0L;

        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        long todayOrders = orderRepository != null ? orderRepository.countOrdersBetweenDates(startOfDay, endOfDay) : 0L;
        
        Map<String, Object> data = new HashMap<>();
        data.put("totalUsers", totalUsers);
        data.put("totalProducts", totalProducts);
        data.put("totalShops", totalShops);
        data.put("totalOrders", totalOrders);
        data.put("todayOrders", todayOrders);
        data.put("systemHealth", "Operational");
        
        return data;
    }

    private Map<String, Object> getSellerDashboardData(User user) {
        Map<String, Object> data = new HashMap<>();
        
        if (user.getShop() != null) {
            Long shopId = user.getShop().getId();
            long totalProducts = productRepository != null ? productRepository.findByShopId(shopId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements() : 0L;
            long totalOrders = orderRepository != null ? orderRepository.findByShopId(shopId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements() : 0L;

            long pendingOrders = 0L;
            if (orderRepository != null) {
                pendingOrders = orderRepository.findByOrderStatus(Order.OrderStatus.PLACED, org.springframework.data.domain.Pageable.unpaged())
                    .stream()
                    .filter(order -> order.getItems().stream()
                        .anyMatch(item -> item.getProduct().getShop().getId().equals(shopId)))
                    .count();
            }

            LocalDateTime startOfMonth = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIN);
            LocalDateTime endOfMonth = LocalDateTime.now();
            BigDecimal monthlySales = orderRepository != null ? orderRepository.sumRevenueBetweenDates(startOfMonth, endOfMonth) : null;

            data.put("shopName", user.getShop().getShopName());
            data.put("totalProducts", totalProducts);
            data.put("totalOrders", totalOrders);
            data.put("pendingOrders", pendingOrders);
            data.put("monthlySales", monthlySales != null ? "₹" + monthlySales : "₹0");
            data.put("shopStatus", user.getShop().getActive() ? "Active" : "Inactive");
        } else {
            data.put("shopName", "No Shop");
            data.put("totalProducts", 0);
            data.put("totalOrders", 0);
            data.put("pendingOrders", 0);
            data.put("monthlySales", "₹0");
            data.put("shopStatus", "Not Created");
        }
        
        return data;
    }

    private Map<String, Object> getCustomerDashboardData(User user) {
        Long userId = user.getId();
        long totalOrders = orderRepository != null ? orderRepository.countByCustomerId(userId) : 0L;
        BigDecimal totalSpent = orderRepository != null ? orderRepository.sumTotalAmountByCustomerId(userId) : null;
        
        int cartItemsCount = 0;
        if (user.getCart() != null && user.getCart().getItems() != null) {
            cartItemsCount = user.getCart().getItems().size();
        }
        
        long pendingOrders = 0L;
        if (orderRepository != null) {
            pendingOrders = orderRepository.findByCustomerId(userId, org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .filter(order -> order.getOrderStatus() == Order.OrderStatus.PLACED || 
                               order.getOrderStatus() == Order.OrderStatus.CONFIRMED ||
                               order.getOrderStatus() == Order.OrderStatus.PACKED)
                .count();
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("totalOrders", totalOrders);
        data.put("pendingOrders", pendingOrders);
        data.put("cartItems", cartItemsCount);
        data.put("totalSpent", totalSpent != null ? "₹" + totalSpent : "₹0");
        data.put("accountStatus", user.getActive() ? "Active" : "Inactive");
        data.put("emailVerified", user.getEmailVerified());
        
        return data;
    }

    private Map<String, Object> getDeliveryAgentDashboardData(User user) {
        Long agentId = user.getId();
        long totalAssignedDeliveries = orderRepository != null ? orderRepository.findByDeliveryAgentId(agentId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements() : 0L;

        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        long completedToday = 0L;
        if (orderRepository != null) {
            completedToday = orderRepository.findByDeliveryAgentId(agentId, org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .filter(order -> order.getOrderStatus() == Order.OrderStatus.DELIVERED &&
                               order.getUpdatedAt() != null &&
                               order.getUpdatedAt().isAfter(startOfDay) &&
                               order.getUpdatedAt().isBefore(endOfDay))
                .count();
        }

        long pendingPickups = 0L;
        if (orderRepository != null) {
            pendingPickups = orderRepository.findByDeliveryAgentId(agentId, org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .filter(order -> order.getOrderStatus() == Order.OrderStatus.SHIPPED)
                .count();
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("assignedDeliveries", totalAssignedDeliveries);
        data.put("completedToday", completedToday);
        data.put("pendingPickups", pendingPickups);
        data.put("todayEarnings", "₹0");
        data.put("averageRating", "N/A");
        
        return data;
    }
}