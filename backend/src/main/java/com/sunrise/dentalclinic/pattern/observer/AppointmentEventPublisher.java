package com.sunrise.dentalclinic.pattern.observer;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Observer pattern "subject". Spring collects every bean implementing
 * {@link AppointmentObserver} into the constructor-injected list
 * automatically, so adding a new notification channel (push notifications,
 * a webhook...) is a matter of writing one new {@code @Component} class -
 * {@code AppointmentService} never changes.
 */
@Component
public class AppointmentEventPublisher {

    private final List<AppointmentObserver> observers;

    public AppointmentEventPublisher(List<AppointmentObserver> observers) {
        this.observers = observers;
    }

    public void publish(AppointmentEvent event) {
        for (AppointmentObserver observer : observers) {
            observer.onAppointmentEvent(event);
        }
    }
}
