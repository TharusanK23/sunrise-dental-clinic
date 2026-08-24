package com.sunrise.dentalclinic.service;

import com.sunrise.dentalclinic.dto.request.RegisterAppointmentRequest;
import com.sunrise.dentalclinic.dto.response.AppointmentResponse;

import java.util.List;

public interface AppointmentService {
    AppointmentResponse register(RegisterAppointmentRequest request, String createdByUsername);
    AppointmentResponse findByAppointmentNumber(String appointmentNumber);
    List<AppointmentResponse> findAll();
    AppointmentResponse cancel(String appointmentNumber);
    AppointmentResponse updateStatus(String appointmentNumber, String status);
}
