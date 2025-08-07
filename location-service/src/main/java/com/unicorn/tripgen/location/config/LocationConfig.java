package com.unicorn.tripgen.location.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Location Service 설정
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.unicorn.tripgen.location.repository")
@EnableTransactionManagement
public class LocationConfig {
    
    /**
     * Location Service 속성 설정
     */
    @Bean
    @ConfigurationProperties(prefix = "location.service")
    public LocationServiceProperties locationServiceProperties() {
        return new LocationServiceProperties();
    }
    
    /**
     * Location Service 속성 클래스
     */
    public static class LocationServiceProperties {
        private Cache cache = new Cache();
        private Search search = new Search();
        private External external = new External();
        
        // Getters and setters
        public Cache getCache() { return cache; }
        public void setCache(Cache cache) { this.cache = cache; }
        
        public Search getSearch() { return search; }
        public void setSearch(Search search) { this.search = search; }
        
        public External getExternal() { return external; }
        public void setExternal(External external) { this.external = external; }
        
        public static class Cache {
            private int defaultTtl = 1800; // 30분
            private int searchResultTtl = 300; // 5분
            private int locationDetailTtl = 1800; // 30분
            private int weatherInfoTtl = 900; // 15분
            private int routeInfoTtl = 600; // 10분
            
            // Getters and setters
            public int getDefaultTtl() { return defaultTtl; }
            public void setDefaultTtl(int defaultTtl) { this.defaultTtl = defaultTtl; }
            
            public int getSearchResultTtl() { return searchResultTtl; }
            public void setSearchResultTtl(int searchResultTtl) { this.searchResultTtl = searchResultTtl; }
            
            public int getLocationDetailTtl() { return locationDetailTtl; }
            public void setLocationDetailTtl(int locationDetailTtl) { this.locationDetailTtl = locationDetailTtl; }
            
            public int getWeatherInfoTtl() { return weatherInfoTtl; }
            public void setWeatherInfoTtl(int weatherInfoTtl) { this.weatherInfoTtl = weatherInfoTtl; }
            
            public int getRouteInfoTtl() { return routeInfoTtl; }
            public void setRouteInfoTtl(int routeInfoTtl) { this.routeInfoTtl = routeInfoTtl; }
        }
        
        public static class Search {
            private int defaultPageSize = 20;
            private int maxPageSize = 50;
            private int maxRadius = 50000; // 50km
            private int defaultRadius = 5000; // 5km
            
            // Getters and setters
            public int getDefaultPageSize() { return defaultPageSize; }
            public void setDefaultPageSize(int defaultPageSize) { this.defaultPageSize = defaultPageSize; }
            
            public int getMaxPageSize() { return maxPageSize; }
            public void setMaxPageSize(int maxPageSize) { this.maxPageSize = maxPageSize; }
            
            public int getMaxRadius() { return maxRadius; }
            public void setMaxRadius(int maxRadius) { this.maxRadius = maxRadius; }
            
            public int getDefaultRadius() { return defaultRadius; }
            public void setDefaultRadius(int defaultRadius) { this.defaultRadius = defaultRadius; }
        }
        
        public static class External {
            private int timeoutSeconds = 30;
            private int retryCount = 3;
            private boolean fallbackEnabled = true;
            
            // Getters and setters
            public int getTimeoutSeconds() { return timeoutSeconds; }
            public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
            
            public int getRetryCount() { return retryCount; }
            public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
            
            public boolean isFallbackEnabled() { return fallbackEnabled; }
            public void setFallbackEnabled(boolean fallbackEnabled) { this.fallbackEnabled = fallbackEnabled; }
        }
    }
}