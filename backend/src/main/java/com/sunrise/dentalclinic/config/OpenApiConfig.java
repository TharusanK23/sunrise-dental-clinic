package com.sunrise.dentalclinic.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers a JWT "bearerAuth" scheme with springdoc so Swagger UI shows an
 * "Authorize" button: paste the {@code token} field from
 * {@code POST /api/auth/login}'s response body there, and every subsequent
 * "Try it out" call sends it as {@code Authorization: Bearer <token>} -
 * {@link com.sunrise.dentalclinic.security.JwtAuthenticationFilter} accepts
 * that header as an alternative to the browser's HttpOnly cookie.
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI dentalClinicOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sunrise Dental Clinic API")
                        .description("CIS6003 coursework - Online Appointment & Patient Management System REST API. "
                                + "Authenticate via POST /api/auth/login, then click Authorize and paste the "
                                + "returned \"token\" value to authorise further requests.")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SCHEME_NAME,
                        new SecurityScheme()
                                .name(SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
