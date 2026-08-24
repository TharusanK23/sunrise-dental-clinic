package com.sunrise.dentalclinic.config;

import com.sunrise.dentalclinic.pattern.singleton.AppConfigManager;
import com.sunrise.dentalclinic.pattern.singleton.AppointmentNumberGenerator;
import com.sunrise.dentalclinic.repository.AppointmentRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Bridges Spring's dependency-injected configuration into the two plain-Java
 * Singletons ({@link AppConfigManager}, {@link AppointmentNumberGenerator})
 * exactly once at boot, so that the appointment-number sequence continues
 * correctly across application restarts instead of resetting to zero.
 */
@Component
public class AppStartupInitializer implements ApplicationRunner {

    private final AppointmentRepository appointmentRepository;
    private final BusinessProperties businessProperties;

    public AppStartupInitializer(AppointmentRepository appointmentRepository, BusinessProperties businessProperties) {
        this.appointmentRepository = appointmentRepository;
        this.businessProperties = businessProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        long currentCount = appointmentRepository.count();
        AppointmentNumberGenerator.getInstance().initialise(currentCount);

        AppConfigManager config = AppConfigManager.getInstance();
        config.set("currencySymbol", businessProperties.getCurrencySymbol());
        config.set("taxRate", String.valueOf(businessProperties.getTaxRate()));
        config.set("systemName", "Sunrise Dental Clinic - Online Appointment & Patient Management System");
    }
}
