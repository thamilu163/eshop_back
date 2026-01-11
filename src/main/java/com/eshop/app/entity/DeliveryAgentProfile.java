package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "delivery_agent_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAgentProfile extends BaseEntity {

    @Column(name = "vehicle_type", length = 100)
    private String vehicleType;

    @Column(name = "vehicle_number", length = 50)
    private String vehicleNumber;

    @Column(name = "license_number", length = 100)
    private String licenseNumber;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
}
