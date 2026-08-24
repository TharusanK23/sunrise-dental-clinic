package com.sunrise.dentalclinic.service.impl;

import com.sunrise.dentalclinic.dto.request.RegisterAppointmentRequest;
import com.sunrise.dentalclinic.dto.response.AppointmentResponse;
import com.sunrise.dentalclinic.entity.Appointment;
import com.sunrise.dentalclinic.entity.AppointmentStatus;
import com.sunrise.dentalclinic.entity.Dentist;
import com.sunrise.dentalclinic.entity.DentistStatus;
import com.sunrise.dentalclinic.entity.Patient;
import com.sunrise.dentalclinic.entity.TreatmentType;
import com.sunrise.dentalclinic.entity.User;
import com.sunrise.dentalclinic.exception.BusinessRuleException;
import com.sunrise.dentalclinic.exception.ResourceNotFoundException;
import com.sunrise.dentalclinic.pattern.builder.AppointmentBuilder;
import com.sunrise.dentalclinic.pattern.observer.AppointmentEvent;
import com.sunrise.dentalclinic.pattern.observer.AppointmentEventPublisher;
import com.sunrise.dentalclinic.repository.AppointmentRepository;
import com.sunrise.dentalclinic.repository.DentistRepository;
import com.sunrise.dentalclinic.repository.PatientRepository;
import com.sunrise.dentalclinic.repository.TreatmentTypeRepository;
import com.sunrise.dentalclinic.repository.UserRepository;
import com.sunrise.dentalclinic.service.AppointmentService;
import com.sunrise.dentalclinic.util.DtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Implements "Register New Appointment" and "Display Appointment Details"
 * from the brief. Combines the {@link AppointmentBuilder} (Builder),
 * {@link AppointmentEventPublisher} (Observer) and the repository layer
 * (DAO) to fulfil Task B's design-pattern and database requirements
 * together in one cohesive workflow.
 */
@Service
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DentistRepository dentistRepository;
    private final TreatmentTypeRepository treatmentTypeRepository;
    private final UserRepository userRepository;
    private final AppointmentEventPublisher eventPublisher;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                   PatientRepository patientRepository,
                                   DentistRepository dentistRepository,
                                   TreatmentTypeRepository treatmentTypeRepository,
                                   UserRepository userRepository,
                                   AppointmentEventPublisher eventPublisher) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.dentistRepository = dentistRepository;
        this.treatmentTypeRepository = treatmentTypeRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public AppointmentResponse register(RegisterAppointmentRequest request, String createdByUsername) {
        Patient patient = resolvePatient(request);

        Dentist dentist = dentistRepository.findById(request.dentistId())
                .orElseThrow(() -> new ResourceNotFoundException("Dentist not found with id: " + request.dentistId()));

        if (dentist.getStatus() == DentistStatus.ON_LEAVE || dentist.getStatus() == DentistStatus.INACTIVE) {
            throw new BusinessRuleException("Dr. " + dentist.getFullName() + " is not available for booking (status: " + dentist.getStatus() + ").");
        }

        TreatmentType treatmentType = treatmentTypeRepository.findById(request.treatmentTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Treatment type not found with id: " + request.treatmentTypeId()));

        List<Appointment> conflicting = appointmentRepository.findConflictingForDentist(
                dentist.getId(), request.appointmentDate(), request.appointmentTime());
        if (!conflicting.isEmpty()) {
            throw new BusinessRuleException("Dr. " + dentist.getFullName()
                    + " already has an appointment at this date/time (existing appointment "
                    + conflicting.get(0).getAppointmentNumber() + "). This check is also enforced at the database level by trg_prevent_double_booking.");
        }

        User createdBy = userRepository.findByUsername(createdByUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found: " + createdByUsername));

        Appointment appointment = new AppointmentBuilder()
                .withPatient(patient)
                .withDentist(dentist)
                .withTreatmentType(treatmentType)
                .withSchedule(request.appointmentDate(), request.appointmentTime())
                .withNotes(request.notes())
                .withCreatedBy(createdBy)
                .withStatus(AppointmentStatus.CONFIRMED)
                .build();

        appointment = appointmentRepository.save(appointment);

        eventPublisher.publish(new AppointmentEvent(appointment, AppointmentEvent.AppointmentEventType.CREATED));

        return DtoMapper.toResponse(appointment);
    }

    private Patient resolvePatient(RegisterAppointmentRequest request) {
        if (request.patientId() != null) {
            return patientRepository.findById(request.patientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + request.patientId()));
        }
        if (!StringUtils.hasText(request.patientFullName()) || !StringUtils.hasText(request.patientAddress())
                || !StringUtils.hasText(request.patientContactNumber())) {
            throw new BusinessRuleException("Either an existing patientId or the patient's full name, address and contact number must be provided.");
        }
        Patient patient = Patient.builder()
                .fullName(request.patientFullName())
                .address(request.patientAddress())
                .contactNumber(request.patientContactNumber())
                .email(request.patientEmail())
                .build();
        return patientRepository.save(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse findByAppointmentNumber(String appointmentNumber) {
        return appointmentRepository.findByAppointmentNumber(appointmentNumber)
                .map(DtoMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No appointment found with number: " + appointmentNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> findAll() {
        return appointmentRepository.findAll().stream().map(DtoMapper::toResponse).toList();
    }

    @Override
    public AppointmentResponse cancel(String appointmentNumber) {
        Appointment appointment = appointmentRepository.findByAppointmentNumber(appointmentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("No appointment found with number: " + appointmentNumber));

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessRuleException("A completed appointment cannot be cancelled.");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);

        eventPublisher.publish(new AppointmentEvent(appointment, AppointmentEvent.AppointmentEventType.CANCELLED));
        return DtoMapper.toResponse(appointment);
    }

    @Override
    public AppointmentResponse updateStatus(String appointmentNumber, String status) {
        Appointment appointment = appointmentRepository.findByAppointmentNumber(appointmentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("No appointment found with number: " + appointmentNumber));

        AppointmentStatus newStatus;
        try {
            newStatus = AppointmentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Unknown appointment status: " + status);
        }

        appointment.setStatus(newStatus);
        appointmentRepository.save(appointment);

        eventPublisher.publish(new AppointmentEvent(appointment, AppointmentEvent.AppointmentEventType.CONFIRMED));
        return DtoMapper.toResponse(appointment);
    }
}
