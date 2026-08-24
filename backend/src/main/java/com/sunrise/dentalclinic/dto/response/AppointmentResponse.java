package com.sunrise.dentalclinic.dto.response;

import com.sunrise.dentalclinic.entity.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AppointmentResponse(
        Long id,
        String appointmentNumber,
        PatientResponse patient,
        DentistResponse dentist,
        TreatmentTypeResponse treatmentType,
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        AppointmentStatus status,
        String notes,
        String createdByUsername,
        LocalDateTime createdAt
) {
}
