package com.eshop.app.service.impl;

import com.eshop.app.dto.request.StoreCreateRequest;
import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.dto.response.StoreResponse;
import com.eshop.app.entity.Store;
import com.eshop.app.entity.User;
import com.eshop.app.exception.ResourceAlreadyExistsException;
import com.eshop.app.exception.ResourceNotFoundException;
import com.eshop.app.mapper.StoreMapper;
import com.eshop.app.repository.StoreRepository;
import com.eshop.app.repository.UserRepository;
import com.eshop.app.service.StoreService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final StoreMapper storeMapper;

    public StoreServiceImpl(StoreRepository storeRepository,
            UserRepository userRepository,
            StoreMapper storeMapper) {
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.storeMapper = storeMapper;
    }

    private Long getCurrentUserId() {
        return com.eshop.app.util.SecurityUtils.getAuthenticatedUserId();
    }

    @Override
    public StoreResponse createStore(StoreCreateRequest request) {
        if (storeRepository.existsByStoreName(request.getStoreName())) {
            throw new ResourceAlreadyExistsException("Store with name " + request.getStoreName() + " already exists");
        }

        User seller = userRepository.findById(request.getSellerId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

        if (seller.getRole() != User.UserRole.SELLER) {
            throw new IllegalArgumentException("User is not a seller");
        }

        if (storeRepository.findBySellerId(seller.getId()).isPresent()) {
            throw new ResourceAlreadyExistsException("Seller already has a store");
        }

        Store store = Store.builder()
                .storeName(request.getStoreName())
                .description(request.getDescription())
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .logoUrl(request.getLogoUrl())
                .seller(seller)
                .sellerType(request.getSellerType())
                .active(true)
                .build();

        store = storeRepository.save(store);
        return storeMapper.toStoreResponse(store);
    }

    @Override
    public StoreResponse updateStore(Long id, StoreCreateRequest request) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + id));

        if (!store.getStoreName().equals(request.getStoreName()) &&
                storeRepository.existsByStoreName(request.getStoreName())) {
            throw new ResourceAlreadyExistsException("Store with name " + request.getStoreName() + " already exists");
        }

        store.setStoreName(request.getStoreName());
        store.setDescription(request.getDescription());
        store.setAddress(request.getAddress());
        store.setPhone(request.getPhone());
        store.setEmail(request.getEmail());
        store.setLogoUrl(request.getLogoUrl());
        store.setSellerType(request.getSellerType());

        store = storeRepository.save(store);
        return storeMapper.toStoreResponse(store);
    }

    @Override
    public void deleteStore(Long id) {
        if (!storeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Store not found with id: " + id);
        }
        storeRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreResponse getStoreById(Long id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + id));
        return storeMapper.toStoreResponse(store);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreResponse getMyStore() {
        Long sellerId = getCurrentUserId();
        Store store = storeRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found for current seller"));
        return storeMapper.toStoreResponse(store);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StoreResponse> getAllStores(Pageable pageable) {
        Page<Store> storePage = storeRepository.findAll(pageable);
        return PageResponse.of(storePage, storeMapper::toStoreResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StoreResponse> searchStores(String keyword, Pageable pageable) {
        Page<Store> storePage = storeRepository.searchStores(keyword, pageable);
        return PageResponse.of(storePage, storeMapper::toStoreResponse);
    }

    // Dashboard Analytics Methods Implementation
    @Override
    @Transactional(readOnly = true)
    public long getTotalStoreCount() {
        return storeRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public String getStoreNameBySellerId(Long sellerId) {
        Store store = storeRepository.findBySellerId(sellerId)
                .orElse(null);
        return store != null ? store.getStoreName() : "N/A";
    }

    @Override
    @Transactional(readOnly = true)
    public Double getStoreRatingBySellerId(Long sellerId) {
        Store store = storeRepository.findBySellerId(sellerId)
                .orElse(null);
        return store != null && store.getRating() != null ? store.getRating() : 0.0;
    }
}
