package com.unicorn.tripgen.trip.biz.domain;

import java.util.Objects;

/**
 * 날씨 정보 값 객체 (Value Object)
 */
public class WeatherInfo {
    private final String condition;
    private final double minTemperature;
    private final double maxTemperature;
    private final String icon;
    
    private WeatherInfo(String condition, double minTemperature, double maxTemperature, String icon) {
        this.condition = Objects.requireNonNull(condition, "날씨 상태는 필수입니다");
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
        this.icon = icon;
        
        // 온도 유효성 검증
        if (minTemperature > maxTemperature) {
            throw new IllegalArgumentException("최저 온도가 최고 온도보다 높을 수 없습니다");
        }
    }
    
    /**
     * 날씨 정보 생성 팩토리 메서드
     */
    public static WeatherInfo of(String condition, double minTemperature, double maxTemperature, String icon) {
        return new WeatherInfo(condition, minTemperature, maxTemperature, icon);
    }
    
    /**
     * 아이콘 없는 날씨 정보 생성
     */
    public static WeatherInfo of(String condition, double minTemperature, double maxTemperature) {
        return new WeatherInfo(condition, minTemperature, maxTemperature, null);
    }
    
    /**
     * 평균 온도 계산
     */
    public double getAverageTemperature() {
        return (minTemperature + maxTemperature) / 2.0;
    }
    
    /**
     * 온도 차이 계산
     */
    public double getTemperatureDifference() {
        return maxTemperature - minTemperature;
    }
    
    /**
     * 추운 날씨인지 확인 (최고 온도 기준 10도 미만)
     */
    public boolean isCold() {
        return maxTemperature < 10.0;
    }
    
    /**
     * 더운 날씨인지 확인 (최고 온도 기준 30도 이상)
     */
    public boolean isHot() {
        return maxTemperature >= 30.0;
    }
    
    /**
     * 비 오는 날씨인지 확인
     */
    public boolean isRainy() {
        return condition != null && (
            condition.toLowerCase().contains("rain") ||
            condition.toLowerCase().contains("비") ||
            condition.toLowerCase().contains("shower")
        );
    }
    
    /**
     * 눈 오는 날씨인지 확인
     */
    public boolean isSnowy() {
        return condition != null && (
            condition.toLowerCase().contains("snow") ||
            condition.toLowerCase().contains("눈")
        );
    }
    
    // Getters
    public String getCondition() {
        return condition;
    }
    
    public double getMinTemperature() {
        return minTemperature;
    }
    
    public double getMaxTemperature() {
        return maxTemperature;
    }
    
    public String getIcon() {
        return icon;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WeatherInfo that = (WeatherInfo) o;
        return Double.compare(that.minTemperature, minTemperature) == 0 &&
               Double.compare(that.maxTemperature, maxTemperature) == 0 &&
               Objects.equals(condition, that.condition) &&
               Objects.equals(icon, that.icon);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(condition, minTemperature, maxTemperature, icon);
    }
    
    @Override
    public String toString() {
        return "WeatherInfo{" +
                "condition='" + condition + '\'' +
                ", minTemperature=" + minTemperature +
                ", maxTemperature=" + maxTemperature +
                ", icon='" + icon + '\'' +
                '}';
    }
}