package com.sunrise.dentalclinic.pattern.builder;

import com.sunrise.dentalclinic.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentBuilderTest {

    private final Patient patient = Patient.builder().id(1L).fullName("Kasun Perera").address("Colombo").contactNumber("0771234567").build();
    private final Dentist dentist = Dentist.builder().id(1L).fullName("Dr. Silva").specialization("General").status(DentistStatus.AVAILABLE).build();
    private final TreatmentType treatmentType = TreatmentType.builder().id(1L).treatmentName("Scaling").consultationFee(java.math.BigDecimal.valueOf(2500)).build();
    private final User staff = User.builder().id(1L).username("kirisha").role(Role.STAFF).build();

    @Test
    @DisplayName("Builds a valid CONFIRMED appointment with a generated appointment number when all mandatory fields are supplied")
    void buildsValidAppointment() {
        Appointment appointment = new AppointmentBuilder()
                .withPatient(patient)
                .withDentist(dentist)
                .withTreatmentType(treatmentType)
                .withSchedule(LocalDate.now().plusDays(1), LocalTime.of(10, 0))
                .withCreatedBy(staff)
                .build();

        assertThat(appointment.getAppointmentNumber()).startsWith("APT-");
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(appointment.getPatient()).isEqualTo(patient);
        assertThat(appointment.getDentist()).isEqualTo(dentist);
    }

    @Test
    @DisplayName("Rejects an appointment date in the past")
    void rejectsPastDate() {
        AppointmentBuilder builder = new AppointmentBuilder()
                .withPatient(patient)
                .withDentist(dentist)
                .withTreatmentType(treatmentType)
                .withSchedule(LocalDate.now().minusDays(1), LocalTime.of(10, 0))
                .withCreatedBy(staff);

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("past");
    }

    @Test
    @DisplayName("Rejects a same-day booking whose time has already passed")
    void rejectsPastTimeForSameDayBooking() {
        AppointmentBuilder builder = new AppointmentBuilder()
                .withPatient(patient)
                .withDentist(dentist)
                .withTreatmentType(treatmentType)
                .withSchedule(LocalDate.now(), LocalTime.MIDNIGHT)
                .withCreatedBy(staff);

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("same-day");
    }

    @Test
    @DisplayName("Rejects a build missing a mandatory field (dentist)")
    void rejectsMissingMandatoryField() {
        AppointmentBuilder builder = new AppointmentBuilder()
                .withPatient(patient)
                .withTreatmentType(treatmentType)
                .withSchedule(LocalDate.now().plusDays(1), LocalTime.of(10, 0))
                .withCreatedBy(staff);

        assertThatThrownBy(builder::build).isInstanceOf(IllegalStateException.class);
    }
}
