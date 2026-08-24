package com.sunrise.dentalclinic.pattern.observer;

import com.sunrise.dentalclinic.entity.Appointment;

/** The payload passed from the Subject to every Observer when an appointment's lifecycle changes. */
public record AppointmentEvent(Appointment appointment, AppointmentEventType type) {

    public enum AppointmentEventType {
        CREATED,
        CONFIRMED,
        CANCELLED,
        BILL_GENERATED
    }
}
