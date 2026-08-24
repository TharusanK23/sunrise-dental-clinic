package com.sunrise.dentalclinic;

import com.sunrise.dentalclinic.config.BusinessProperties;
import com.sunrise.dentalclinic.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Entry point for the Sunrise Dental Clinic - Online Appointment & Patient
 * Management System REST API.
 * <p>
 * CIS6003 Advanced Programming coursework. This Spring Boot application
 * exposes a distributed, three-tier (Presentation / Business Logic / Data
 * Access) web service layer consumed by the static HTML/CSS/JS client in the
 * {@code frontend} module.
 */
@SpringBootApplication
@EnableConfigurationProperties({BusinessProperties.class, JwtProperties.class})
public class DentalClinicApplication {

    public static void main(String[] args) {
        SpringApplication.run(DentalClinicApplication.class, args);
    }
}
