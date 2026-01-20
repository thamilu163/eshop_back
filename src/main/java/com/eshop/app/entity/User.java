package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;
import com.eshop.app.enums.UserRole;


import java.util.Set;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_username", columnList = "username")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = { "cart", "store", "orders", "deliveryAgentProfile", "sellerProfile" })
@EqualsAndHashCode(callSuper = true, of = {"email"})
public class User extends BaseEntity {
    
    @Column(nullable = false, unique = true, length = 100)
    private String username;
    
    @Column(nullable = false, unique = true, length = 150)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "first_name", nullable = true, length = 100)
    private String firstName;
    
    @Column(name = "last_name", nullable = true, length = 100)
    private String lastName;
    
    @Column(length = 20)
    private String phone;
    
    @Column(length = 500)
    private String address;
    
    @OneToOne(mappedBy = "seller", fetch = FetchType.LAZY)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private SellerProfile sellerProfile;

    // Assuming Cart and Order entities exist but I don't want to import them if not
    // needed.
    // They are used in ToString exclude so they must exist in the class.

    // I need imports for these too potentially?
    // User.java in Step 113 imports: jakarta.persistence.*, lombok.*,
    // java.util.HashSet, java.util.Set.
    // It does NOT import Cart, Order, etc.
    // If they are in same package com.eshop.app.entity, no import needed.

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Cart cart;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private Set<Order> orders;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private DeliveryAgentProfile deliveryAgentProfile;

    @Column(name = "two_factor_enabled")
    @Builder.Default
    private Boolean twoFactorEnabled = false;

    @Column(name = "two_factor_secret")
    private String twoFactorSecret;

    // Explicit getter for compatibility
    public String getUsername() {
        return this.username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    // Explicit getters for compatibility
    public UserRole getRole() {
        return this.role;
    }
    public Boolean getActive() {
        return this.active;
    }
    public String getEmail() {
        return this.email;
    }
    public String getPassword() {
        return this.password;
    }
}
