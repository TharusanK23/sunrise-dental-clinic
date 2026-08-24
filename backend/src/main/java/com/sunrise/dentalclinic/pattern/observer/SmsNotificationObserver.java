package com.sunrise.dentalclinic.pattern.observer;

import com.sunrise.dentalclinic.entity.NotificationLog;
import com.sunrise.dentalclinic.repository.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Simulated SMS notification channel - see {@link EmailNotificationObserver} for the simulation rationale. */
@Component
public class SmsNotificationObserver implements AppointmentObserver {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationObserver.class);
    private final NotificationLogRepository notificationLogRepository;

    public SmsNotificationObserver(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    @Override
    public void onAppointmentEvent(AppointmentEvent event) {
        String message = "SMS to %s: Appointment %s is %s.".formatted(
                event.appointment().getPatient().getContactNumber(),
                event.appointment().getAppointmentNumber(),
                event.type().name().toLowerCase()
        );
        log.info("[SMS] {}", message);
        notificationLogRepository.save(NotificationLog.builder()
                .appointmentNumber(event.appointment().getAppointmentNumber())
                .channel("SMS")
                .message(message)
                .build());
    }
}
