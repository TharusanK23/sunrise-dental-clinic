package com.sunrise.dentalclinic.pattern.factory;

import com.sunrise.dentalclinic.config.BusinessProperties;
import com.sunrise.dentalclinic.entity.*;
import com.sunrise.dentalclinic.pattern.strategy.PricingContext;
import com.sunrise.dentalclinic.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillFactoryTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    private BillFactory billFactory;

    private final Patient patient = Patient.builder().id(1L).fullName("Kasun Perera").build();
    private final TreatmentType treatmentType = TreatmentType.builder().id(1L).treatmentName("Scaling").consultationFee(BigDecimal.valueOf(2500)).build();

    @BeforeEach
    void setUp() {
        BusinessProperties props = new BusinessProperties();
        props.setTaxRate(0.08);
        props.setWeekendSurchargeRate(0.15);
        props.setLoyaltyDiscountVisitCount(5);
        props.setLoyaltyDiscountRate(0.10);
        billFactory = new BillFactory(new PricingContext(props), appointmentRepository);
    }

    @Test
    @DisplayName("Creates a Bill from an appointment, deriving an INV- bill number from the appointment number and applying STANDARD pricing for a new patient on a weekday")
    void createsBillForNewPatientOnWeekday() {
        Appointment appointment = Appointment.builder()
                .appointmentNumber("APT-2026-000001")
                .patient(patient)
                .treatmentType(treatmentType)
                .appointmentDate(LocalDate.of(2026, 8, 25)) // Tuesday
                .appointmentTime(LocalTime.of(10, 0))
                .status(AppointmentStatus.CONFIRMED)
                .build();

        when(appointmentRepository.countCompletedByPatient(1L)).thenReturn(0L);

        Bill bill = billFactory.create(appointment);

        assertThat(bill.getBillNumber()).isEqualTo("INV-2026-000001");
        assertThat(bill.getPricingStrategy()).isEqualTo("STANDARD");
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("2700.00");
        assertThat(bill.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    @Test
    @DisplayName("Applies the loyalty discount strategy when the patient has 5+ completed prior visits")
    void createsBillWithLoyaltyDiscountForReturningPatient() {
        Appointment appointment = Appointment.builder()
                .appointmentNumber("APT-2026-000042")
                .patient(patient)
                .treatmentType(treatmentType)
                .appointmentDate(LocalDate.of(2026, 8, 25))
                .appointmentTime(LocalTime.of(10, 0))
                .status(AppointmentStatus.CONFIRMED)
                .build();

        when(appointmentRepository.countCompletedByPatient(1L)).thenReturn(6L);

        Bill bill = billFactory.create(appointment);

        assertThat(bill.getBillNumber()).isEqualTo("INV-2026-000042");
        assertThat(bill.getPricingStrategy()).isEqualTo("LOYALTY_DISCOUNT");
        assertThat(bill.getDiscountAmount()).isEqualByComparingTo("250.00");
    }
}
