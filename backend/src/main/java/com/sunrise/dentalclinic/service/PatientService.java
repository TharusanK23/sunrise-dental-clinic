package com.sunrise.dentalclinic.service;

import com.sunrise.dentalclinic.dto.response.PatientResponse;

import java.util.List;

public interface PatientService {
    List<PatientResponse> findAll();
    PatientResponse findById(Long id);
    List<PatientResponse> search(String name);
}
