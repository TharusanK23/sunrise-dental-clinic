package com.sunrise.dentalclinic.service;

import com.sunrise.dentalclinic.dto.request.CreateDentistRequest;
import com.sunrise.dentalclinic.dto.response.DentistResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface DentistService {
    List<DentistResponse> findAll();
    DentistResponse findById(Long id);
    DentistResponse create(CreateDentistRequest request);
    void delete(Long id);
    List<DentistResponse> findAvailable(LocalDate appointmentDate, LocalTime appointmentTime);
}
