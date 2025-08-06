package com.unicorn.tripgen.location.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger 설정
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Location Service API")
                        .description("Location Service REST API 문서")
                        .version("1.0.0"))
                .servers(List.of(
                        new Server().url("http://localhost:8082").description("Local server")
                ));
    }
}