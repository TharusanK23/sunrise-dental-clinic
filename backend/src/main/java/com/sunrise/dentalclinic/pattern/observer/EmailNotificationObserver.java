package com.sunrise.dentalclinic.pattern.observer;

import com.sunrise.dentalclinic.entity.NotificationLog;
import com.sunrise.dentalclinic.repository.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Simulated e-mail notification channel. No real SMTP gateway is configured
 * for this coursework build (see docs/SETUP.md, "Notification simulation");
 * the message that would be sent to the patient is logged and persisted to
 * {@code notification_logs} as verifiable evidence that the alert fired.
 */
@Component
public class EmailNotificationObserver implements AppointmentObserver {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationObserver.class);
    private final NotificationLogRepository notificationLogRepository;

    public EmailNotificationObserver(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    @Override
    public void onAppointmentEvent(AppointmentEvent event) {
        String message = "Dear %s, your appointment %s has been %s. Dentist: %s (%s), on %s at %s.".formatted(
                event.appointment().getPatient().getFullName(),
                event.appointment().getAppointmentNumber(),
                event.type().name().toLowerCase(),
                event.appointment().getDentist().getFullName(),
                event.appointment().getTreatmentType().getTreatmentName(),
                event.appointment().getAppointmentDate(),
                event.appointment().getAppointmentTime()
        );
        log.info("[EMAIL] To: {} | {}", event.appointment().getPatient().getEmail(), message);
        notificationLogRepository.save(NotificationLog.builder()
                .appointmentNumber(event.appointment().getAppointmentNumber())
                .channel("EMAIL")
                .message(message)
                .build());
    }
}
