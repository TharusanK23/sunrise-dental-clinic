package com.sunrise.dentalclinic.util;

import com.sunrise.dentalclinic.dto.response.*;
import com.sunrise.dentalclinic.entity.*;

/** Pure, stateless mapping functions between JPA entities and the DTOs exposed over the REST API. */
public final class DtoMapper {

    private DtoMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(), user.getRole());
    }

    public static PatientResponse toResponse(Patient patient) {
        return new PatientResponse(patient.getId(), patient.getFullName(), patient.getAddress(),
                patient.getContactNumber(), patient.getEmail(), patient.getDateOfBirth());
    }

    public static TreatmentTypeResponse toResponse(TreatmentType treatmentType) {
        return new TreatmentTypeResponse(treatmentType.getId(), treatmentType.getTreatmentName(),
                treatmentType.getConsultationFee(), treatmentType.getDescription());
    }

    public static DentistResponse toResponse(Dentist dentist) {
        return new DentistResponse(dentist.getId(), dentist.getFullName(), dentist.getSpecialization(),
                dentist.getContactNumber(), dentist.getStatus());
    }

    public static AppointmentResponse toResponse(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getAppointmentNumber(),
                toResponse(appointment.getPatient()),
                toResponse(appointment.getDentist()),
                toResponse(appointment.getTreatmentType()),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime(),
                appointment.getStatus(),
                appointment.getNotes(),
                appointment.getCreatedBy().getUsername(),
                appointment.getCreatedAt()
        );
    }

    public static BillResponse toResponse(Bill bill) {
        return new BillResponse(
                bill.getId(),
                bill.getBillNumber(),
                toResponse(bill.getAppointment()),
                bill.getConsultationFee(),
                bill.getSubtotal(),
                bill.getSurchargeAmount(),
                bill.getDiscountAmount(),
                bill.getTaxAmount(),
                bill.getTotalAmount(),
                bill.getPricingStrategy(),
                bill.getPaymentStatus(),
                bill.getPaymentMethod(),
                bill.getGeneratedAt()
        );
    }
}
