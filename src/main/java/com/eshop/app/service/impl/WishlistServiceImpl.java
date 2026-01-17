package com.eshop.app.service.impl;

import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.dto.response.WishlistResponse;
import com.eshop.app.entity.Product;
import com.eshop.app.entity.User;
import com.eshop.app.entity.Wishlist;
import com.eshop.app.exception.ResourceNotFoundException;
import com.eshop.app.repository.ProductRepository;
import com.eshop.app.repository.UserRepository;
import com.eshop.app.repository.WishlistRepository;
import com.eshop.app.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    public WishlistResponse addToWishlist(Long userId, Long productId, String notes) {
        // Check if already exists
        Optional<Wishlist> existing = wishlistRepository.findByUserIdAndProductId(userId, productId);
        if (existing.isPresent()) {
            return mapToResponse(existing.get());
        }

        // Verify user and product exist
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with id: {}", userId);
                    return new ResourceNotFoundException("User not found with id: " + userId);
                });
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Product not found with id: {}", productId);
                    return new ResourceNotFoundException("Product not found with id: " + productId);
                });

        // Create wishlist item
        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .notes(notes)
                .build();

        Wishlist saved = wishlistRepository.save(wishlist);
        return mapToResponse(saved);
    }

    @Override
    public void removeFromWishlist(Long userId, Long productId) {
        Wishlist wishlist = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found"));
        wishlistRepository.delete(wishlist);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInWishlist(Long userId, Long productId) {
        return wishlistRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WishlistResponse> getUserWishlist(Long userId, Pageable pageable) {
        Page<Wishlist> page = wishlistRepository.findByUserId(userId, pageable);
        return toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistResponse> getUserWishlistItems(Long userId) {
        List<Wishlist> wishlists = wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return wishlists.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistResponse> getUserWishlistWithDetails(Long userId) {
        List<Wishlist> wishlists = wishlistRepository.findByUserIdWithProductDetails(userId);
        return wishlists.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistResponse> getUserWishlistByStore(Long userId, Long storeId) {
        List<Wishlist> wishlists = wishlistRepository.findByUserIdAndStoreId(userId, storeId);
        return wishlists.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistResponse> getUserWishlistByCategory(Long userId, Long categoryId) {
        List<Wishlist> wishlists = wishlistRepository.findByUserIdAndCategoryId(userId, categoryId);
        return wishlists.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WishlistResponse> searchUserWishlist(Long userId, String keyword, Pageable pageable) {
        Page<Wishlist> page = wishlistRepository.searchWishlistByProductName(userId, keyword, pageable);
        return toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public long getWishlistCount(Long userId) {
        return wishlistRepository.countByUserId(userId);
    }

    @Override
    public void clearWishlist(Long userId) {
        wishlistRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Object> getMostWishlistedProducts(int limit) {
        return (List<Object>) (List<?>) wishlistRepository.getMostWishlistedProducts(PageRequest.of(0, limit));
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Object> getWishlistStatisticsByCategory() {
        return (List<Object>) (List<?>) wishlistRepository.getWishlistStatisticsByCategory();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object> getUsersInterestedInStore(Long storeId) {
        return wishlistRepository.findUsersWhoWishlistedFromStore(storeId);
    }

    @Override
    public WishlistResponse updateWishlistNotes(Long userId, Long productId, String notes) {
        Wishlist wishlist = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found"));
        wishlist.setNotes(notes);
        Wishlist updated = wishlistRepository.save(wishlist);
        return mapToResponse(updated);
    }

    @Override
    public List<Object> moveWishlistToCart(Long userId, List<Long> productIds) {
        // Implementation would require CartService dependency
        // For now, return empty list as placeholder
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object> getWishlistRecommendations(Long userId) {
        // Implementation for ML-based recommendations
        // For now, return empty list as placeholder
        return List.of();
    }

    private WishlistResponse mapToResponse(Wishlist wishlist) {
        Product product = wishlist.getProduct();

        WishlistResponse.ProductDetails productDetails = null;
        if (product != null) {
            productDetails = WishlistResponse.ProductDetails.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .description(product.getDescription())
                    .price(product.getPrice())
                    .discountPrice(product.getDiscountPrice())
                    .imageUrl(getPrimaryImageUrl(product))
                    .isActive(isProductActive(product))
                    .stockQuantity(product.getStockQuantity())
                    .inStock(product.getStockQuantity() != null && product.getStockQuantity() > 0)
                    .storeName(product.getStore() != null ? product.getStore().getStoreName() : null)
                    .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                    .isAvailable(isProductActive(product) && product.getStockQuantity() != null
                            && product.getStockQuantity() > 0)
                    .availabilityMessage(isProductActive(product)
                            ? (product.getStockQuantity() != null && product.getStockQuantity() > 0 ? "In Stock"
                                    : "Out of Stock")
                            : "Product Not Available")
                    .build();
        }

        return WishlistResponse.builder()
                .id(wishlist.getId())
                .userId(wishlist.getUser().getId())
                .productId(wishlist.getProduct().getId())
                .notes(wishlist.getNotes())
                .createdAt(wishlist.getCreatedAt())
                .product(productDetails)
                .build();
    }

    private PageResponse<WishlistResponse> toPageResponse(Page<Wishlist> page) {
        List<WishlistResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        PageResponse.PageMetadata metadata = PageResponse.PageMetadata.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
        return PageResponse.<WishlistResponse>builder()
                .data(content)
                .pagination(metadata)
                .build();
    }

    // Helper methods to avoid deprecated API calls
    private String getPrimaryImageUrl(Product product) {
        if (product == null) {
            return null;
        }
        if (product.getPrimaryImage() != null) {
            return product.getPrimaryImage().getUrl();
        }
        // Safely check images collection initialization to avoid
        // LazyInitializationException
        try {
            if (product.getImages() != null && org.hibernate.Hibernate.isInitialized(product.getImages())
                    && !product.getImages().isEmpty()) {
                com.eshop.app.entity.ProductImage img = product.getImages().get(0);
                if (img != null && img.getUrl() != null) {
                    return img.getUrl();
                }
            }
        } catch (Exception e) {
            // ignore and return null
        }
        return null;
    }

    private boolean isProductActive(Product product) {
        if (product == null) {
            return false;
        }
        return product.getStatus() == com.eshop.app.entity.enums.ProductStatus.ACTIVE && !product.isDeleted();
    }
}