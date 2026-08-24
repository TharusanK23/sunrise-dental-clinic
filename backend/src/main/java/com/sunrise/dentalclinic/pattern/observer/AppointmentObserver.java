package com.sunrise.dentalclinic.pattern.observer;

/** Observer pattern: implementations react to an {@link AppointmentEvent} without the Subject knowing how many, or which, observers exist. */
public interface AppointmentObserver {
    void onAppointmentEvent(AppointmentEvent event);
}
