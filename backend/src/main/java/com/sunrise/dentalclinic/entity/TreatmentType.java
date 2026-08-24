package com.sunrise.dentalclinic.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A treatment type (Consultation, Scaling, Filling, Root Canal, Extraction ...)
 * each with its own consultation fee, exactly as named in the brief:
 * "Calculate the total treatment cost based on treatment type and consultation fee."
 */
@Entity
@Table(name = "treatment_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TreatmentType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "treatment_name", nullable = false, unique = true, length = 50)
    private String treatmentName;

    @Column(name = "consultation_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal consultationFee;

    @Column(length = 255)
    private String description;
}
