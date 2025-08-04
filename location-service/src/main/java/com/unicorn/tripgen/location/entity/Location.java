package com.unicorn.tripgen.location.entity;

import com.unicorn.tripgen.common.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "locations", indexes = {
    @Index(name = "idx_location_type", columnList = "location_type"),
    @Index(name = "idx_location_coordinates", columnList = "latitude,longitude")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location extends BaseAuditEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "location_id")
    private String locationId;
    
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 20)
    private LocationType locationType;
    
    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;
    
    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;
    
    @Column(name = "address", length = 500)
    private String address;
    
    @Column(name = "postal_code", length = 20)
    private String postalCode;
    
    @Column(name = "phone", length = 50)
    private String phone;
    
    @Column(name = "website", length = 500)
    private String website;
    
    @Column(name = "opening_hours", columnDefinition = "TEXT")
    private String openingHours;
    
    @Column(name = "rating", precision = 3, scale = 2)
    private BigDecimal rating;
    
    @Column(name = "review_count")
    private Integer reviewCount;
    
    @Column(name = "price_level")
    private Integer priceLevel;
    
    @Column(name = "image_url", length = 1000)
    private String imageUrl;
    
    @Column(name = "place_id", length = 100, unique = true)
    private String placeId; // External API place ID
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    // Helper method
    public String getExternalId() {
        return this.placeId;
    }
}