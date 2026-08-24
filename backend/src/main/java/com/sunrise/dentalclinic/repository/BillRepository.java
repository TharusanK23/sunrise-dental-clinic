package com.sunrise.dentalclinic.repository;

import com.sunrise.dentalclinic.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {
    Optional<Bill> findByAppointmentId(Long appointmentId);
    Optional<Bill> findByBillNumber(String billNumber);
}
