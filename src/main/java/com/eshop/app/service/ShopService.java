package com.eshop.app.service;

import com.eshop.app.dto.request.ShopCreateRequest;
import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.dto.response.ShopResponse;
import org.springframework.data.domain.Pageable;

public interface ShopService {
    ShopResponse createShop(ShopCreateRequest request);
    ShopResponse updateShop(Long id, ShopCreateRequest request);
    void deleteShop(Long id);
    ShopResponse getShopById(Long id);
    ShopResponse getMyShop();
    PageResponse<ShopResponse> getAllShops(Pageable pageable);
    PageResponse<ShopResponse> searchShops(String keyword, Pageable pageable);
    
    // Dashboard Analytics Methods
    long getTotalShopCount();
    String getShopNameBySellerId(Long sellerId);
    Double getShopRatingBySellerId(Long sellerId);
}
