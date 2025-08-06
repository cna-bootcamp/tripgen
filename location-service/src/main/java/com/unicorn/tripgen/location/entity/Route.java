package com.unicorn.tripgen.location.entity;

import com.unicorn.tripgen.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "routes", indexes = {
    @Index(name = "idx_route_origin_dest", columnList = "origin_id,destination_id"),
    @Index(name = "idx_route_transport", columnList = "transport_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Route extends BaseEntity {
    
    @Column(name = "route_id", unique = true)
    private String routeId;
    
    @Column(name = "origin_id", nullable = false, length = 100)
    private String originId;
    
    @Column(name = "destination_id", nullable = false, length = 100)
    private String destinationId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type", nullable = false, length = 20)
    private TransportType transportType;
    
    @Column(name = "distance", nullable = false)
    private Integer distance; // in meters
    
    @Column(name = "duration", nullable = false)
    private Integer duration; // in seconds
    
    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(name = "route_polyline", columnDefinition = "TEXT")
    private String routePolyline; // Encoded polyline
    
    @Column(name = "steps", columnDefinition = "TEXT")
    private String steps; // JSON format
    
    @Column(name = "warnings", columnDefinition = "TEXT")
    private String warnings;
}