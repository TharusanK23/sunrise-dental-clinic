package com.sunrise.dentalclinic.repository;

import com.sunrise.dentalclinic.entity.Appointment;
import com.sunrise.dentalclinic.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByAppointmentNumber(String appointmentNumber);

    List<Appointment> findByStatus(AppointmentStatus status);

    List<Appointment> findByPatientId(Long patientId);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.appointmentDate = :date")
    long countByAppointmentDate(@Param("date") LocalDate date);

    /** Used by BillFactory's LoyaltyDiscountPricingStrategy selection: how many prior visits has this patient completed? */
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.patient.id = :patientId AND a.status = com.sunrise.dentalclinic.entity.AppointmentStatus.COMPLETED")
    long countCompletedByPatient(@Param("patientId") Long patientId);

    @Query("""
           SELECT a FROM Appointment a
           WHERE a.dentist.id = :dentistId
             AND a.status IN (com.sunrise.dentalclinic.entity.AppointmentStatus.PENDING,
                               com.sunrise.dentalclinic.entity.AppointmentStatus.CONFIRMED)
             AND a.appointmentDate = :appointmentDate
             AND a.appointmentTime = :appointmentTime
           """)
    List<Appointment> findConflictingForDentist(@Param("dentistId") Long dentistId,
                                                 @Param("appointmentDate") LocalDate appointmentDate,
                                                 @Param("appointmentTime") LocalTime appointmentTime);
}
