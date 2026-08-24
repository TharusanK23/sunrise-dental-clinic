package com.sunrise.dentalclinic.repository;

import com.sunrise.dentalclinic.entity.TreatmentType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TreatmentTypeRepository extends JpaRepository<TreatmentType, Long> {
}
