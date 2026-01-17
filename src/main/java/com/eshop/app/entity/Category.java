package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "categories", indexes = {
    @Index(name = "idx_category_name", columnList = "name")
})
@Getter
@Setter
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(callSuper = true, of = {"name"})
public class Category extends BaseEntity {
    
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(length = 150)
    private String slug;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    // SEO Metadata
    @Embedded
    private SeoMetadata seoMetadata;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @ToString.Exclude
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @Builder.Default
    @ToString.Exclude
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Set<Category> subCategories = new HashSet<>();

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Set<Product> products = new HashSet<>();

    // Convenience constructor used in some service code
    public Category(String name) {
        this.name = name;
    }

    // Explicitly initialize final fields in the no-args constructor
    public Category() {
        this.active = true;
        this.products = new HashSet<>();
    }

    // Explicit getters and setters for compatibility
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return this.description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
}