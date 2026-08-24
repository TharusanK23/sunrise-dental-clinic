package com.sunrise.dentalclinic.pattern.observer;

import com.sunrise.dentalclinic.entity.NotificationLog;
import com.sunrise.dentalclinic.repository.NotificationLogRepository;
import org.springframework.stereotype.Component;

/** Internal audit-trail channel: every appointment lifecycle event is recorded for traceability, independent of patient-facing alerts. */
@Component
public class AuditLogObserver implements AppointmentObserver {

    private final NotificationLogRepository notificationLogRepository;

    public AuditLogObserver(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    @Override
    public void onAppointmentEvent(AppointmentEvent event) {
        String message = "AUDIT: appointment %s changed to %s by staff at %s".formatted(
                event.appointment().getAppointmentNumber(),
                event.type().name(),
                event.appointment().getCreatedBy().getUsername()
        );
        notificationLogRepository.save(NotificationLog.builder()
                .appointmentNumber(event.appointment().getAppointmentNumber())
                .channel("AUDIT")
                .message(message)
                .build());
    }
}
