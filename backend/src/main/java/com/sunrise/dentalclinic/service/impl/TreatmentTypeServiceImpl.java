package com.sunrise.dentalclinic.service.impl;

import com.sunrise.dentalclinic.dto.request.CreateTreatmentTypeRequest;
import com.sunrise.dentalclinic.dto.response.TreatmentTypeResponse;
import com.sunrise.dentalclinic.entity.TreatmentType;
import com.sunrise.dentalclinic.exception.DuplicateResourceException;
import com.sunrise.dentalclinic.exception.ResourceNotFoundException;
import com.sunrise.dentalclinic.repository.TreatmentTypeRepository;
import com.sunrise.dentalclinic.service.TreatmentTypeService;
import com.sunrise.dentalclinic.util.DtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TreatmentTypeServiceImpl implements TreatmentTypeService {

    private final TreatmentTypeRepository treatmentTypeRepository;

    public TreatmentTypeServiceImpl(TreatmentTypeRepository treatmentTypeRepository) {
        this.treatmentTypeRepository = treatmentTypeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentTypeResponse> findAll() {
        return treatmentTypeRepository.findAll().stream().map(DtoMapper::toResponse).toList();
    }

    @Override
    public TreatmentTypeResponse create(CreateTreatmentTypeRequest request) {
        boolean exists = treatmentTypeRepository.findAll().stream()
                .anyMatch(t -> t.getTreatmentName().equalsIgnoreCase(request.treatmentName()));
        if (exists) {
            throw new DuplicateResourceException("A treatment type named '" + request.treatmentName() + "' already exists.");
        }
        TreatmentType treatmentType = TreatmentType.builder()
                .treatmentName(request.treatmentName())
                .consultationFee(request.consultationFee())
                .description(request.description())
                .build();
        return DtoMapper.toResponse(treatmentTypeRepository.save(treatmentType));
    }

    @Override
    public void delete(Long id) {
        if (!treatmentTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Treatment type not found with id: " + id);
        }
        treatmentTypeRepository.deleteById(id);
    }
}
