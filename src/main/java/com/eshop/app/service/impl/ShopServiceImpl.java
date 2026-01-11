package com.eshop.app.service.impl;

import com.eshop.app.dto.request.ShopCreateRequest;
import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.dto.response.ShopResponse;
import com.eshop.app.entity.Shop;
import com.eshop.app.entity.User;
import com.eshop.app.exception.ResourceAlreadyExistsException;
import com.eshop.app.exception.ResourceNotFoundException;
import com.eshop.app.mapper.ShopMapper;
import com.eshop.app.repository.ShopRepository;
import com.eshop.app.repository.UserRepository;
import com.eshop.app.security.UserDetailsImpl;
import com.eshop.app.service.ShopService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional
public class ShopServiceImpl implements ShopService {
    
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final ShopMapper shopMapper;
    
    public ShopServiceImpl(ShopRepository shopRepository,
                          UserRepository userRepository,
                          ShopMapper shopMapper) {
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.shopMapper = shopMapper;
    }
    
    private Long getCurrentUserId() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userDetails.getId();
    }
    
    @Override
    public ShopResponse createShop(ShopCreateRequest request) {
        if (shopRepository.existsByShopName(request.getShopName())) {
            throw new ResourceAlreadyExistsException("Shop with name " + request.getShopName() + " already exists");
        }
        
        User seller = userRepository.findById(request.getSellerId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
        
        if (seller.getRole() != User.UserRole.SELLER) {
            throw new IllegalArgumentException("User is not a seller");
        }
        
        if (shopRepository.findBySellerId(seller.getId()).isPresent()) {
            throw new ResourceAlreadyExistsException("Seller already has a shop");
        }
        
        Shop shop = Shop.builder()
                .shopName(request.getShopName())
                .description(request.getDescription())
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .logoUrl(request.getLogoUrl())
                .seller(seller)
                .sellerType(request.getSellerType())
                .active(true)
                .build();
        
        shop = shopRepository.save(shop);
        return shopMapper.toShopResponse(shop);
    }
    
    @Override
    public ShopResponse updateShop(Long id, ShopCreateRequest request) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + id));
        
        if (!shop.getShopName().equals(request.getShopName()) && 
            shopRepository.existsByShopName(request.getShopName())) {
            throw new ResourceAlreadyExistsException("Shop with name " + request.getShopName() + " already exists");
        }
        
        shop.setShopName(request.getShopName());
        shop.setDescription(request.getDescription());
        shop.setAddress(request.getAddress());
        shop.setPhone(request.getPhone());
        shop.setEmail(request.getEmail());
        shop.setLogoUrl(request.getLogoUrl());
        shop.setSellerType(request.getSellerType());
        
        shop = shopRepository.save(shop);
        return shopMapper.toShopResponse(shop);
    }
    
    @Override
    public void deleteShop(Long id) {
        if (!shopRepository.existsById(id)) {
            throw new ResourceNotFoundException("Shop not found with id: " + id);
        }
        shopRepository.deleteById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ShopResponse getShopById(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + id));
        return shopMapper.toShopResponse(shop);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ShopResponse getMyShop() {
        Long sellerId = getCurrentUserId();
        Shop shop = shopRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found for current seller"));
        return shopMapper.toShopResponse(shop);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ShopResponse> getAllShops(Pageable pageable) {
        Page<Shop> shopPage = shopRepository.findAll(pageable);
        return PageResponse.of(shopPage, shopMapper::toShopResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ShopResponse> searchShops(String keyword, Pageable pageable) {
        Page<Shop> shopPage = shopRepository.searchShops(keyword, pageable);
        return PageResponse.of(shopPage, shopMapper::toShopResponse);
    }
    
    // Dashboard Analytics Methods Implementation
    @Override
    @Transactional(readOnly = true)
    public long getTotalShopCount() {
        return shopRepository.count();
    }
    
    @Override
    @Transactional(readOnly = true)
    public String getShopNameBySellerId(Long sellerId) {
        Shop shop = shopRepository.findBySellerId(sellerId)
                .orElse(null);
        return shop != null ? shop.getShopName() : "N/A";
    }
    
    @Override
    @Transactional(readOnly = true)
    public Double getShopRatingBySellerId(Long sellerId) {
        Shop shop = shopRepository.findBySellerId(sellerId)
                .orElse(null);
        return shop != null && shop.getRating() != null ? shop.getRating() : 0.0;
    }
}
