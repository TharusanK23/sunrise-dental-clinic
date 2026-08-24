package com.sunrise.dentalclinic.service.impl;

import com.sunrise.dentalclinic.dto.request.CreateDentistRequest;
import com.sunrise.dentalclinic.dto.response.DentistResponse;
import com.sunrise.dentalclinic.entity.Dentist;
import com.sunrise.dentalclinic.exception.ResourceNotFoundException;
import com.sunrise.dentalclinic.repository.DentistRepository;
import com.sunrise.dentalclinic.service.DentistService;
import com.sunrise.dentalclinic.util.DtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional
public class DentistServiceImpl implements DentistService {

    private final DentistRepository dentistRepository;

    public DentistServiceImpl(DentistRepository dentistRepository) {
        this.dentistRepository = dentistRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DentistResponse> findAll() {
        return dentistRepository.findAll().stream().map(DtoMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DentistResponse findById(Long id) {
        return dentistRepository.findById(id)
                .map(DtoMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Dentist not found with id: " + id));
    }

    @Override
    public DentistResponse create(CreateDentistRequest request) {
        Dentist dentist = Dentist.builder()
                .fullName(request.fullName())
                .specialization(request.specialization())
                .contactNumber(request.contactNumber())
                .build();
        return DtoMapper.toResponse(dentistRepository.save(dentist));
    }

    @Override
    public void delete(Long id) {
        if (!dentistRepository.existsById(id)) {
            throw new ResourceNotFoundException("Dentist not found with id: " + id);
        }
        dentistRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DentistResponse> findAvailable(LocalDate appointmentDate, LocalTime appointmentTime) {
        return dentistRepository.findAvailableDentists(appointmentDate, appointmentTime)
                .stream().map(DtoMapper::toResponse).toList();
    }
}
