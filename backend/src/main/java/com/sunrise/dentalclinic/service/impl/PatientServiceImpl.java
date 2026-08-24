package com.sunrise.dentalclinic.service.impl;

import com.sunrise.dentalclinic.dto.response.PatientResponse;
import com.sunrise.dentalclinic.exception.ResourceNotFoundException;
import com.sunrise.dentalclinic.repository.PatientRepository;
import com.sunrise.dentalclinic.service.PatientService;
import com.sunrise.dentalclinic.util.DtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;

    public PatientServiceImpl(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public List<PatientResponse> findAll() {
        return patientRepository.findAll().stream().map(DtoMapper::toResponse).toList();
    }

    @Override
    public PatientResponse findById(Long id) {
        return patientRepository.findById(id)
                .map(DtoMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));
    }

    @Override
    public List<PatientResponse> search(String name) {
        return patientRepository.findByFullNameContainingIgnoreCase(name).stream().map(DtoMapper::toResponse).toList();
    }
}
