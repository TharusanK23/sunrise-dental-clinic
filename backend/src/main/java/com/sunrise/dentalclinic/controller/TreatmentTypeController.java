package com.sunrise.dentalclinic.controller;

import com.sunrise.dentalclinic.dto.request.CreateTreatmentTypeRequest;
import com.sunrise.dentalclinic.dto.response.TreatmentTypeResponse;
import com.sunrise.dentalclinic.service.TreatmentTypeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/treatment-types")
public class TreatmentTypeController {

    private final TreatmentTypeService treatmentTypeService;

    public TreatmentTypeController(TreatmentTypeService treatmentTypeService) {
        this.treatmentTypeService = treatmentTypeService;
    }

    @GetMapping
    public List<TreatmentTypeResponse> findAll() {
        return treatmentTypeService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TreatmentTypeResponse create(@Valid @RequestBody CreateTreatmentTypeRequest request) {
        return treatmentTypeService.create(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        treatmentTypeService.delete(id);
    }
}
