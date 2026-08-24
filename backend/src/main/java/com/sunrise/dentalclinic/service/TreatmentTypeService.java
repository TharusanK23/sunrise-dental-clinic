package com.sunrise.dentalclinic.service;

import com.sunrise.dentalclinic.dto.request.CreateTreatmentTypeRequest;
import com.sunrise.dentalclinic.dto.response.TreatmentTypeResponse;

import java.util.List;

public interface TreatmentTypeService {
    List<TreatmentTypeResponse> findAll();
    TreatmentTypeResponse create(CreateTreatmentTypeRequest request);
    void delete(Long id);
}
