package com.unicorn.tripgen.location.repository;

import com.unicorn.tripgen.location.entity.Location;
import com.unicorn.tripgen.location.entity.LocationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, String> {
    
    Optional<Location> findByPlaceId(String placeId);
    
    @Query(value = """
        SELECT l.* FROM locations l 
        WHERE l.is_active = true 
        AND ST_DWithin(
            ST_MakePoint(CAST(l.longitude AS DOUBLE PRECISION), CAST(l.latitude AS DOUBLE PRECISION))::geography,
            ST_MakePoint(:longitude, :latitude)::geography,
            :radius
        )
        ORDER BY ST_Distance(
            ST_MakePoint(CAST(l.longitude AS DOUBLE PRECISION), CAST(l.latitude AS DOUBLE PRECISION))::geography,
            ST_MakePoint(:longitude, :latitude)::geography
        )
        LIMIT :limit
        """, nativeQuery = true)
    List<Location> findNearbyLocations(
        @Param("latitude") double latitude,
        @Param("longitude") double longitude,
        @Param("radius") double radius,
        @Param("limit") int limit
    );
    
    @Query(value = """
        SELECT l.* FROM locations l 
        WHERE l.is_active = true 
        AND l.location_type = :locationType
        AND ST_DWithin(
            ST_MakePoint(CAST(l.longitude AS DOUBLE PRECISION), CAST(l.latitude AS DOUBLE PRECISION))::geography,
            ST_MakePoint(:longitude, :latitude)::geography,
            :radius
        )
        ORDER BY ST_Distance(
            ST_MakePoint(CAST(l.longitude AS DOUBLE PRECISION), CAST(l.latitude AS DOUBLE PRECISION))::geography,
            ST_MakePoint(:longitude, :latitude)::geography
        )
        LIMIT :limit
        """, nativeQuery = true)
    List<Location> findNearbyLocationsByType(
        @Param("latitude") double latitude,
        @Param("longitude") double longitude,
        @Param("radius") double radius,
        @Param("locationType") String locationType,
        @Param("limit") int limit
    );
    
    @Query("SELECT l FROM Location l WHERE l.isActive = true " +
           "AND LOWER(l.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY l.rating DESC NULLS LAST")
    List<Location> searchByKeyword(@Param("keyword") String keyword);
    
    List<Location> findByLocationTypeAndIsActiveTrue(LocationType locationType);
    
    Optional<Location> findByPlaceIdIgnoreCase(String placeId);
    
    @Query(value = """
        SELECT l FROM Location l 
        WHERE l.isActive = true 
        AND l.locationType = :locationType
        AND FUNCTION('ST_DWithin', 
            FUNCTION('ST_MakePoint', l.longitude, l.latitude), 
            FUNCTION('ST_MakePoint', :longitude, :latitude), 
            :radius) = true
        ORDER BY FUNCTION('ST_Distance', 
            FUNCTION('ST_MakePoint', l.longitude, l.latitude), 
            FUNCTION('ST_MakePoint', :longitude, :latitude))
        """)
    Page<Location> findLocationsByTypeWithinRadius(
        @Param("latitude") BigDecimal latitude,
        @Param("longitude") BigDecimal longitude,
        @Param("radius") Double radius,
        @Param("locationType") String locationType,
        Pageable pageable
    );
    
    @Query(value = """
        SELECT l FROM Location l 
        WHERE l.isActive = true 
        AND FUNCTION('ST_DWithin', 
            FUNCTION('ST_MakePoint', l.longitude, l.latitude), 
            FUNCTION('ST_MakePoint', :longitude, :latitude), 
            :radius) = true
        ORDER BY FUNCTION('ST_Distance', 
            FUNCTION('ST_MakePoint', l.longitude, l.latitude), 
            FUNCTION('ST_MakePoint', :longitude, :latitude))
        """)
    Page<Location> findLocationsWithinRadius(
        @Param("latitude") BigDecimal latitude,
        @Param("longitude") BigDecimal longitude,
        @Param("radius") Double radius,
        Pageable pageable
    );
    
    Page<Location> findByIsActiveTrueOrderByReviewCountDesc(Pageable pageable);
    
    @Query("SELECT l FROM Location l WHERE l.isActive = true AND l.rating >= 4.0 ORDER BY l.rating DESC")
    Page<Location> findTopRatedLocations(Pageable pageable);
}