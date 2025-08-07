package com.unicorn.tripgen.trip.infra.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
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
                        .title("Trip Service API")
                        .description("Trip Service REST API 문서 - 여행 일정 관리 서비스")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Trip Service Team")
                                .email("trip@tripgen.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8083").description("Local Development Server"),
                        new Server().url("https://api.tripgen.com").description("Production Server")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Bearer 토큰을 입력하세요")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}