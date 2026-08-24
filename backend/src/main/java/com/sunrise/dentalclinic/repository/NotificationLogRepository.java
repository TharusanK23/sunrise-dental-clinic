package com.sunrise.dentalclinic.repository;

import com.sunrise.dentalclinic.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByAppointmentNumber(String appointmentNumber);
}
