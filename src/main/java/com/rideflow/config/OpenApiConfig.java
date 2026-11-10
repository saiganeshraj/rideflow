package com.rideflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI rideFlowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RideFlow API")
                        .description("Real-Time Ride Booking Backend API")
                        .version("0.1.0")
                        .contact(new Contact()
                                .name("RideFlow Team"))
                        .license(new License()
                                .name("MIT")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT Bearer token")));
    }
}
