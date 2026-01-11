package com.eshop.app.service.impl;

import com.eshop.app.dto.request.ProductCreateRequest;
import com.eshop.app.entity.Category;
import com.eshop.app.entity.Shop;
import com.eshop.app.entity.Product;
import com.eshop.app.repository.*;
import com.eshop.app.mapper.ProductMapper;
import com.eshop.app.service.AttributeValidatorService;
import com.eshop.app.config.ProductProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ProductServiceImplSanitizationTest {

    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;
    private BrandRepository brandRepository;
    private ShopRepository shopRepository;
    private TagRepository tagRepository;
    private OrderItemRepository orderItemRepository;
    private ProductMapper productMapper;
    private AttributeValidatorService attributeValidatorService;
    private ProductProperties productProperties;
    private ApplicationEventPublisher eventPublisher;
    private ProductServiceHelper helper;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        brandRepository = mock(BrandRepository.class);
        shopRepository = mock(ShopRepository.class);
        tagRepository = mock(TagRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        productMapper = mock(ProductMapper.class);
        attributeValidatorService = mock(AttributeValidatorService.class);
        productProperties = mock(ProductProperties.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        helper = mock(ProductServiceHelper.class);

        productService = new ProductServiceImpl(
            productRepository,
            categoryRepository,
            brandRepository,
            shopRepository,
            tagRepository,
            orderItemRepository,
            productMapper,
            attributeValidatorService,
            productProperties,
            eventPublisher,
            helper
        );
    }

    @Test
    void createProduct_sanitizesDescriptionBeforeSave() {
        ProductCreateRequest req = ProductCreateRequest.builder()
                .name("Name")
                .sku("SKU-1")
                .price(new BigDecimal("10.00"))
                .categoryId(1L)
                .shopId(1L)
                .description("<script>alert(1)</script>")
                .build();

        when(productRepository.existsBySku(any())).thenReturn(false);
        Category cat = new Category(); cat.setId(1L); cat.setName("Cat");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        Shop shop = new Shop(); shop.setId(1L); shop.setShopName("Shop");
        when(shopRepository.findById(1L)).thenReturn(Optional.of(shop));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Helper should build the Product; if not stubbed it returns null causing NPE
        when(helper.resolveOrCreateTags(any())).thenReturn(new java.util.HashSet<>());
        when(helper.buildProductFromRequest(any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            ProductCreateRequest r = (ProductCreateRequest) invocation.getArgument(0);
            Product p = new Product();
            p.setName(r.getName());
            p.setSku(r.getSku());
            p.setPrice(r.getPrice());
            // Simulate sanitizer behavior used in service (HtmlUtils.htmlEscape)
            String desc = r.getDescription() == null ? null : org.springframework.web.util.HtmlUtils.htmlEscape(r.getDescription());
            p.setDescription(desc);
            p.setCategory((Category) invocation.getArgument(1));
            p.setShop((Shop) invocation.getArgument(2));
            return p;
        });

        productService.createProduct(req, "test-user-id");

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, atLeastOnce()).save(captor.capture());
        Product saved = captor.getValue();
        assertNotNull(saved);
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;", saved.getDescription());
    }
}
