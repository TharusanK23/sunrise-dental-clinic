package com.sunrise.dentalclinic.pattern.builder;

import com.sunrise.dentalclinic.entity.Appointment;
import com.sunrise.dentalclinic.entity.AppointmentStatus;
import com.sunrise.dentalclinic.entity.Dentist;
import com.sunrise.dentalclinic.entity.Patient;
import com.sunrise.dentalclinic.entity.TreatmentType;
import com.sunrise.dentalclinic.entity.User;
import com.sunrise.dentalclinic.pattern.singleton.AppointmentNumberGenerator;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Builder pattern: assembles an {@link Appointment} through a fluent,
 * step-by-step API instead of a telescoping constructor. Centralises the
 * cross-field validation rule (the appointment must not be booked in the
 * past) in one place so every entry point that creates an appointment -
 * today the REST controller, tomorrow perhaps a bulk-import job - is
 * guaranteed to build a consistent, valid object graph.
 */
public class AppointmentBuilder {

    private Patient patient;
    private Dentist dentist;
    private TreatmentType treatmentType;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String notes;
    private User createdBy;
    private AppointmentStatus status = AppointmentStatus.CONFIRMED;

    public AppointmentBuilder withPatient(Patient patient) {
        this.patient = patient;
        return this;
    }

    public AppointmentBuilder withDentist(Dentist dentist) {
        this.dentist = dentist;
        return this;
    }

    public AppointmentBuilder withTreatmentType(TreatmentType treatmentType) {
        this.treatmentType = treatmentType;
        return this;
    }

    public AppointmentBuilder withSchedule(LocalDate date, LocalTime time) {
        this.appointmentDate = date;
        this.appointmentTime = time;
        return this;
    }

    public AppointmentBuilder withNotes(String notes) {
        this.notes = notes;
        return this;
    }

    public AppointmentBuilder withCreatedBy(User createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public AppointmentBuilder withStatus(AppointmentStatus status) {
        this.status = status;
        return this;
    }

    public Appointment build() {
        if (patient == null || dentist == null || treatmentType == null
                || appointmentDate == null || appointmentTime == null || createdBy == null) {
            throw new IllegalStateException("Patient, dentist, treatment type, appointment date/time and createdBy are mandatory.");
        }
        LocalDate today = LocalDate.now();
        if (appointmentDate.isBefore(today)) {
            throw new IllegalStateException("Appointment date cannot be in the past.");
        }
        if (appointmentDate.isEqual(today) && appointmentTime.isBefore(LocalTime.now())) {
            throw new IllegalStateException("Appointment time cannot be in the past for a same-day booking.");
        }

        return Appointment.builder()
                .appointmentNumber(AppointmentNumberGenerator.getInstance().nextAppointmentNumber())
                .patient(patient)
                .dentist(dentist)
                .treatmentType(treatmentType)
                .appointmentDate(appointmentDate)
                .appointmentTime(appointmentTime)
                .notes(notes)
                .createdBy(createdBy)
                .status(status)
                .build();
    }
}
