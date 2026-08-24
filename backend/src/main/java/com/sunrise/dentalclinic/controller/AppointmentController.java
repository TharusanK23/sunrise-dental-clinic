package com.sunrise.dentalclinic.controller;

import com.sunrise.dentalclinic.dto.request.RegisterAppointmentRequest;
import com.sunrise.dentalclinic.dto.response.AppointmentResponse;
import com.sunrise.dentalclinic.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * "Register New Appointment" and "Display Appointment Details" from the
 * brief. Search-by-number is exposed at
 * GET /api/appointments/{appointmentNumber}.
 */
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public List<AppointmentResponse> findAll() {
        return appointmentService.findAll();
    }

    @GetMapping("/{appointmentNumber}")
    public AppointmentResponse findByNumber(@PathVariable String appointmentNumber) {
        return appointmentService.findByAppointmentNumber(appointmentNumber);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse register(@Valid @RequestBody RegisterAppointmentRequest request, Authentication authentication) {
        return appointmentService.register(request, authentication.getName());
    }

    @PostMapping("/{appointmentNumber}/cancel")
    public AppointmentResponse cancel(@PathVariable String appointmentNumber) {
        return appointmentService.cancel(appointmentNumber);
    }

    @PatchMapping("/{appointmentNumber}/status")
    public AppointmentResponse updateStatus(@PathVariable String appointmentNumber, @RequestBody Map<String, String> body) {
        return appointmentService.updateStatus(appointmentNumber, body.get("status"));
    }
}
