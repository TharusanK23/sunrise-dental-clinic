package com.sunrise.dentalclinic.controller;

import com.sunrise.dentalclinic.dto.request.CreateDentistRequest;
import com.sunrise.dentalclinic.dto.response.DentistResponse;
import com.sunrise.dentalclinic.service.DentistService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/dentists")
public class DentistController {

    private final DentistService dentistService;

    public DentistController(DentistService dentistService) {
        this.dentistService = dentistService;
    }

    @GetMapping
    public List<DentistResponse> findAll() {
        return dentistService.findAll();
    }

    @GetMapping("/{id}")
    public DentistResponse findById(@PathVariable Long id) {
        return dentistService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DentistResponse create(@Valid @RequestBody CreateDentistRequest request) {
        return dentistService.create(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        dentistService.delete(id);
    }

    @GetMapping("/available")
    public List<DentistResponse> findAvailable(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appointmentDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime appointmentTime) {
        return dentistService.findAvailable(appointmentDate, appointmentTime);
    }
}
