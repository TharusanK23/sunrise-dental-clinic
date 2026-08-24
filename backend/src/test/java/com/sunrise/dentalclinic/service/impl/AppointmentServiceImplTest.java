package com.sunrise.dentalclinic.service.impl;

import com.sunrise.dentalclinic.dto.request.RegisterAppointmentRequest;
import com.sunrise.dentalclinic.dto.response.AppointmentResponse;
import com.sunrise.dentalclinic.entity.*;
import com.sunrise.dentalclinic.exception.BusinessRuleException;
import com.sunrise.dentalclinic.exception.ResourceNotFoundException;
import com.sunrise.dentalclinic.pattern.observer.AppointmentEventPublisher;
import com.sunrise.dentalclinic.repository.AppointmentRepository;
import com.sunrise.dentalclinic.repository.DentistRepository;
import com.sunrise.dentalclinic.repository.PatientRepository;
import com.sunrise.dentalclinic.repository.TreatmentTypeRepository;
import com.sunrise.dentalclinic.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private DentistRepository dentistRepository;
    @Mock private TreatmentTypeRepository treatmentTypeRepository;
    @Mock private UserRepository userRepository;
    @Mock private AppointmentEventPublisher eventPublisher;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private final Patient patient = Patient.builder().id(1L).fullName("Kasun Perera").address("Colombo").contactNumber("0771234567").build();
    private final Dentist dentist = Dentist.builder().id(1L).fullName("Silva").status(DentistStatus.AVAILABLE).build();
    private final TreatmentType treatmentType = TreatmentType.builder().id(1L).treatmentName("Scaling").consultationFee(BigDecimal.valueOf(2500)).build();
    private final User staff = User.builder().id(1L).username("kirisha").role(Role.STAFF).build();

    private RegisterAppointmentRequest requestFor(Long patientId) {
        return new RegisterAppointmentRequest(patientId, null, null, null, null, 1L, 1L,
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), "First visit");
    }

    @Test
    @DisplayName("Registers an appointment successfully when the dentist is available and there is no scheduling conflict")
    void registersAppointmentSuccessfully() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(dentistRepository.findById(1L)).thenReturn(Optional.of(dentist));
        when(treatmentTypeRepository.findById(1L)).thenReturn(Optional.of(treatmentType));
        when(appointmentRepository.findConflictingForDentist(eq(1L), any(), any())).thenReturn(Collections.emptyList());
        when(userRepository.findByUsername("kirisha")).thenReturn(Optional.of(staff));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = appointmentService.register(requestFor(1L), "kirisha");

        assertThat(response.appointmentNumber()).startsWith("APT-");
        assertThat(response.status()).isEqualTo(AppointmentStatus.CONFIRMED);
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("Rejects registration when the dentist is on leave")
    void rejectsUnavailableDentist() {
        Dentist onLeave = Dentist.builder().id(1L).fullName("Silva").status(DentistStatus.ON_LEAVE).build();
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(dentistRepository.findById(1L)).thenReturn(Optional.of(onLeave));

        assertThatThrownBy(() -> appointmentService.register(requestFor(1L), "kirisha"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not available");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rejects registration when the dentist already has a conflicting appointment at that date/time")
    void rejectsDoubleBooking() {
        Appointment existing = Appointment.builder().appointmentNumber("APT-2026-000001").build();
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(dentistRepository.findById(1L)).thenReturn(Optional.of(dentist));
        when(treatmentTypeRepository.findById(1L)).thenReturn(Optional.of(treatmentType));
        when(appointmentRepository.findConflictingForDentist(eq(1L), any(), any())).thenReturn(List.of(existing));

        assertThatThrownBy(() -> appointmentService.register(requestFor(1L), "kirisha"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already has an appointment");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rejects registration when neither a patientId nor the new-patient details (name/address/contact) are supplied")
    void rejectsMissingPatientDetails() {
        RegisterAppointmentRequest incomplete = new RegisterAppointmentRequest(null, null, null, null, null, 1L, 1L,
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), null);

        assertThatThrownBy(() -> appointmentService.register(incomplete, "kirisha"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("patientId");

        verifyNoInteractions(dentistRepository);
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when the dentist id does not exist")
    void rejectsUnknownDentist() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(dentistRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.register(requestFor(1L), "kirisha"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Appointment fullAppointment(AppointmentStatus status) {
        return Appointment.builder()
                .appointmentNumber("APT-2026-000001")
                .patient(patient)
                .dentist(dentist)
                .treatmentType(treatmentType)
                .appointmentDate(LocalDate.now().plusDays(1))
                .appointmentTime(LocalTime.of(10, 0))
                .createdBy(staff)
                .status(status)
                .build();
    }

    @Test
    @DisplayName("Cancels a CONFIRMED appointment and publishes a CANCELLED event")
    void cancelsAppointment() {
        Appointment appointment = fullAppointment(AppointmentStatus.CONFIRMED);
        when(appointmentRepository.findByAppointmentNumber("APT-2026-000001")).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = appointmentService.cancel("APT-2026-000001");

        assertThat(response.status()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("Rejects cancelling an appointment that is already COMPLETED")
    void rejectsCancellingCompletedAppointment() {
        Appointment appointment = fullAppointment(AppointmentStatus.COMPLETED);
        when(appointmentRepository.findByAppointmentNumber("APT-2026-000001")).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.cancel("APT-2026-000001"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be cancelled");
    }
}
