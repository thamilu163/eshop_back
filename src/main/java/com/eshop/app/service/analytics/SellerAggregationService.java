package com.eshop.app.service.analytics;

import com.eshop.app.dto.response.SellerDashboardResponse;
import com.eshop.app.service.OrderService;
import com.eshop.app.service.ProductService;
import com.eshop.app.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class SellerAggregationService {
    private static final Logger log = LoggerFactory.getLogger(SellerAggregationService.class);

    private final ProductService productService;
    private final OrderService orderService;
    private final ShopService shopService;
    private final Executor dashboardExecutor;

    public SellerDashboardResponse.ShopOverview buildShopOverview(Long sellerId) {
        try {
            return SellerDashboardResponse.ShopOverview.builder()
                    .shopName(shopService.getShopNameBySellerId(sellerId))
                    .shopStatus("Active")
                    .totalProducts(productService.getProductCountBySellerId(sellerId))
                    .activeProducts(productService.getActiveProductCountBySellerId(sellerId))
                    .outOfStockProducts(productService.getOutOfStockCountBySellerId(sellerId))
                    .shopRating(shopService.getShopRatingBySellerId(sellerId))
                    .build();
        } catch (Exception e) {
            log.error("Failed to build shop overview for {}: {}", sellerId, e.getMessage(), e);
            return SellerDashboardResponse.ShopOverview.builder().build();
        }
    }

    public SellerDashboardResponse.SalesMetrics buildSalesMetrics(Long sellerId) {
        try {
            return SellerDashboardResponse.SalesMetrics.builder()
                    .todaySales(orderService.getTodayRevenueBySellerId(sellerId))
                    .weeklySales(orderService.getWeeklyRevenueBySellerId(sellerId))
                    .monthlySales(orderService.getMonthlyRevenueBySellerId(sellerId))
                    .totalSales(orderService.getTotalRevenueBySellerId(sellerId))
                    .build();
        } catch (Exception e) {
            log.error("Failed to build sales metrics for {}: {}", sellerId, e.getMessage(), e);
            return SellerDashboardResponse.SalesMetrics.builder().build();
        }
    }

    public SellerDashboardResponse.OrderManagement buildOrderManagement(Long sellerId) {
        try {
            return SellerDashboardResponse.OrderManagement.builder()
                    .newOrders(orderService.getNewOrderCountBySellerId(sellerId))
                    .processingOrders(orderService.getProcessingOrderCountBySellerId(sellerId))
                    .shippedOrders(orderService.getShippedOrderCountBySellerId(sellerId))
                    .completedOrders(orderService.getCompletedOrderCountBySellerId(sellerId))
                    .build();
        } catch (Exception e) {
            log.error("Failed to build order management for {}: {}", sellerId, e.getMessage(), e);
            return SellerDashboardResponse.OrderManagement.builder().build();
        }
    }

    public CompletableFuture<SellerDashboardResponse.ShopOverview> buildShopOverviewAsync(Long sellerId) {
        return CompletableFuture.supplyAsync(() -> buildShopOverview(sellerId), dashboardExecutor);
    }

    public CompletableFuture<SellerDashboardResponse.SalesMetrics> buildSalesMetricsAsync(Long sellerId) {
        return CompletableFuture.supplyAsync(() -> buildSalesMetrics(sellerId), dashboardExecutor);
    }

    public CompletableFuture<SellerDashboardResponse.OrderManagement> buildOrderManagementAsync(Long sellerId) {
        return CompletableFuture.supplyAsync(() -> buildOrderManagement(sellerId), dashboardExecutor);
    }
}
