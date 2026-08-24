package com.sunrise.dentalclinic.pattern.factory;

import com.sunrise.dentalclinic.entity.Appointment;
import com.sunrise.dentalclinic.entity.Bill;
import com.sunrise.dentalclinic.entity.PaymentStatus;
import com.sunrise.dentalclinic.pattern.singleton.AppointmentNumberGenerator;
import com.sunrise.dentalclinic.pattern.strategy.PricingContext;
import com.sunrise.dentalclinic.pattern.strategy.PricingResult;
import com.sunrise.dentalclinic.repository.AppointmentRepository;
import org.springframework.stereotype.Component;

/**
 * Factory Method pattern: hides the multi-step process of turning an
 * {@link Appointment} into a fully-priced {@link Bill} (looking up the
 * patient's loyalty visit count -&gt; strategy selection via
 * {@link PricingContext} -&gt; bill-number generation via the
 * {@link AppointmentNumberGenerator} singleton -&gt; entity assembly) behind
 * a single {@link #create(Appointment)} call, so {@code BillService} never
 * needs to know how pricing or numbering work internally.
 */
@Component
public class BillFactory implements DocumentFactory<Bill, Appointment> {

    private final PricingContext pricingContext;
    private final AppointmentRepository appointmentRepository;

    public BillFactory(PricingContext pricingContext, AppointmentRepository appointmentRepository) {
        this.pricingContext = pricingContext;
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public Bill create(Appointment appointment) {
        long priorVisits = appointmentRepository.countCompletedByPatient(appointment.getPatient().getId());
        var treatmentType = appointment.getTreatmentType();
        PricingResult pricing = pricingContext.price(treatmentType.getConsultationFee(), priorVisits, appointment.getAppointmentDate());

        return Bill.builder()
                .billNumber(AppointmentNumberGenerator.getInstance().billNumberFor(appointment.getAppointmentNumber()))
                .appointment(appointment)
                .consultationFee(treatmentType.getConsultationFee())
                .subtotal(pricing.subtotal())
                .surchargeAmount(pricing.surchargeAmount())
                .discountAmount(pricing.discountAmount())
                .taxAmount(pricing.taxAmount())
                .totalAmount(pricing.totalAmount())
                .pricingStrategy(pricing.strategyName())
                .paymentStatus(PaymentStatus.UNPAID)
                .build();
    }
}
