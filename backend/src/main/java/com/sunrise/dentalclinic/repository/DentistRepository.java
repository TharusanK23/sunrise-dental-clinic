package com.sunrise.dentalclinic.repository;

import com.sunrise.dentalclinic.entity.Dentist;
import com.sunrise.dentalclinic.entity.DentistStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface DentistRepository extends JpaRepository<Dentist, Long> {

    List<Dentist> findByStatus(DentistStatus status);

    /**
     * Dentists who are not ON_LEAVE/INACTIVE and have no active appointment
     * already booked in the exact same date + time slot.
     */
    @Query("""
           SELECT d FROM Dentist d
           WHERE d.status <> com.sunrise.dentalclinic.entity.DentistStatus.ON_LEAVE
             AND d.status <> com.sunrise.dentalclinic.entity.DentistStatus.INACTIVE
             AND d.id NOT IN (
                 SELECT a.dentist.id FROM Appointment a
                 WHERE a.status IN (com.sunrise.dentalclinic.entity.AppointmentStatus.PENDING,
                                     com.sunrise.dentalclinic.entity.AppointmentStatus.CONFIRMED)
                   AND a.appointmentDate = :appointmentDate
                   AND a.appointmentTime = :appointmentTime
             )
           """)
    List<Dentist> findAvailableDentists(@Param("appointmentDate") LocalDate appointmentDate,
                                         @Param("appointmentTime") LocalTime appointmentTime);
}
