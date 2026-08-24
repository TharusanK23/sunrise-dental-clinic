package com.sunrise.dentalclinic.controller;

import com.sunrise.dentalclinic.dto.response.PatientResponse;
import com.sunrise.dentalclinic.service.PatientService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public List<PatientResponse> findAll(@RequestParam(required = false) String search) {
        return (search == null || search.isBlank()) ? patientService.findAll() : patientService.search(search);
    }

    @GetMapping("/{id}")
    public PatientResponse findById(@PathVariable Long id) {
        return patientService.findById(id);
    }
}
