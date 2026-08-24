package com.sunrise.dentalclinic.entity;

/** Lifecycle state of an appointment, from booking through to completion or cancellation. */
public enum AppointmentStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED
}
