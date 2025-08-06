package com.unicorn.tripgen.location.entity;

import com.unicorn.tripgen.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "weather", indexes = {
    @Index(name = "idx_weather_location_date", columnList = "location_id,forecast_date"),
    @Index(name = "idx_weather_coordinates", columnList = "latitude,longitude")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Weather extends BaseEntity {
    
    @Column(name = "weather_id", unique = true)
    private String weatherId;
    
    @Column(name = "location_id", length = 100)
    private String locationId;
    
    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;
    
    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;
    
    @Column(name = "forecast_date", nullable = false)
    private LocalDateTime forecastDate;
    
    @Column(name = "temperature", nullable = false)
    private BigDecimal temperature; // in Celsius
    
    @Column(name = "feels_like")
    private BigDecimal feelsLike;
    
    @Column(name = "temp_min")
    private BigDecimal tempMin;
    
    @Column(name = "temp_max")
    private BigDecimal tempMax;
    
    @Column(name = "humidity")
    private Integer humidity; // percentage
    
    @Column(name = "pressure")
    private Integer pressure; // hPa
    
    @Column(name = "wind_speed")
    private BigDecimal windSpeed; // m/s
    
    @Column(name = "wind_direction")
    private Integer windDirection; // degrees
    
    @Column(name = "clouds")
    private Integer clouds; // percentage
    
    @Column(name = "rain_volume")
    private BigDecimal rainVolume; // mm
    
    @Column(name = "snow_volume")
    private BigDecimal snowVolume; // mm
    
    @Column(name = "weather_main", length = 50)
    private String weatherMain; // Clear, Clouds, Rain, etc.
    
    @Column(name = "weather_description", length = 200)
    private String weatherDescription;
    
    @Column(name = "weather_icon", length = 10)
    private String weatherIcon;
    
    @Column(name = "visibility")
    private Integer visibility; // meters
    
    @Column(name = "uv_index")
    private BigDecimal uvIndex;
}