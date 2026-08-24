package com.sunrise.dentalclinic.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Payload for "Register New Appointment". Either {@code patientId} is
 * supplied to reuse an existing, previously-registered patient, or the
 * {@code patientFullName}/{@code patientAddress}/{@code patientContactNumber}
 * trio is supplied to register a new one inline - mirroring the brief's
 * requirement to capture patient details as part of booking a visit.
 */
public record RegisterAppointmentRequest(
        Long patientId,

        String patientFullName,
        String patientAddress,

        @Pattern(regexp = "^$|^(\\+94|0)[0-9]{9}$", message = "Contact number must be a valid Sri Lankan mobile/landline number")
        String patientContactNumber,

        @Email(message = "Email must be valid") String patientEmail,

        @NotNull(message = "Dentist is required") Long dentistId,

        @NotNull(message = "Treatment type is required") Long treatmentTypeId,

        @NotNull(message = "Appointment date is required")
        @FutureOrPresent(message = "Appointment date cannot be in the past")
        LocalDate appointmentDate,

        @NotNull(message = "Appointment time is required") LocalTime appointmentTime,

        String notes
) {
}
