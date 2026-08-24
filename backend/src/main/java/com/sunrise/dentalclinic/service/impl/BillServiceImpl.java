package com.sunrise.dentalclinic.service.impl;

import com.sunrise.dentalclinic.dto.request.SettlePaymentRequest;
import com.sunrise.dentalclinic.dto.response.BillResponse;
import com.sunrise.dentalclinic.entity.Appointment;
import com.sunrise.dentalclinic.entity.Bill;
import com.sunrise.dentalclinic.entity.PaymentStatus;
import com.sunrise.dentalclinic.exception.ResourceNotFoundException;
import com.sunrise.dentalclinic.pattern.factory.BillFactory;
import com.sunrise.dentalclinic.pattern.observer.AppointmentEvent;
import com.sunrise.dentalclinic.pattern.observer.AppointmentEventPublisher;
import com.sunrise.dentalclinic.repository.AppointmentRepository;
import com.sunrise.dentalclinic.repository.BillRepository;
import com.sunrise.dentalclinic.service.BillService;
import com.sunrise.dentalclinic.util.DtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements "Calculate and Print Bill". Bill amounts are computed exactly
 * once per appointment via {@link BillFactory} (Factory Method + Strategy)
 * and persisted, so repeated calls to the print/receipt endpoint always
 * return the same figures instead of silently re-pricing an appointment
 * whose treatment fee may since have changed.
 */
@Service
@Transactional
public class BillServiceImpl implements BillService {

    private final BillRepository billRepository;
    private final AppointmentRepository appointmentRepository;
    private final BillFactory billFactory;
    private final AppointmentEventPublisher eventPublisher;

    public BillServiceImpl(BillRepository billRepository, AppointmentRepository appointmentRepository,
                            BillFactory billFactory, AppointmentEventPublisher eventPublisher) {
        this.billRepository = billRepository;
        this.appointmentRepository = appointmentRepository;
        this.billFactory = billFactory;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public BillResponse generateOrFetch(String appointmentNumber) {
        Appointment appointment = appointmentRepository.findByAppointmentNumber(appointmentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("No appointment found with number: " + appointmentNumber));

        Bill bill = billRepository.findByAppointmentId(appointment.getId())
                .orElseGet(() -> {
                    Bill newBill = billFactory.create(appointment);
                    Bill saved = billRepository.save(newBill);
                    eventPublisher.publish(new AppointmentEvent(appointment, AppointmentEvent.AppointmentEventType.BILL_GENERATED));
                    return saved;
                });

        return DtoMapper.toResponse(bill);
    }

    @Override
    public BillResponse settlePayment(String billNumber, SettlePaymentRequest request) {
        Bill bill = billRepository.findByBillNumber(billNumber)
                .orElseThrow(() -> new ResourceNotFoundException("No bill found with number: " + billNumber));
        bill.setPaymentStatus(PaymentStatus.PAID);
        bill.setPaymentMethod(request.paymentMethod());
        return DtoMapper.toResponse(billRepository.save(bill));
    }
}
