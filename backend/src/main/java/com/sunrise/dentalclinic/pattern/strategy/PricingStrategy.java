package com.sunrise.dentalclinic.pattern.strategy;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Strategy pattern: encapsulates one algorithm for turning a treatment
 * type's consultation fee into a priced result. Concrete strategies are
 * interchangeable at runtime - selected by {@link PricingContext} - without
 * the caller (BillService) knowing which one is in effect. This keeps new
 * pricing rules (seasonal rates, insurance discounts...) additive rather
 * than requiring edits to existing billing code (Open/Closed Principle).
 */
public interface PricingStrategy {

    /**
     * @param consultationFee     the {@link com.sunrise.dentalclinic.entity.TreatmentType} consultation fee
     * @param patientVisitCount   the patient's number of prior COMPLETED appointments (loyalty signal)
     * @param appointmentDate     the date of the appointment (used by date-sensitive strategies)
     * @return the computed pricing breakdown
     */
    PricingResult calculate(BigDecimal consultationFee, long patientVisitCount, LocalDate appointmentDate);

    /** Human-readable name recorded on the bill for auditability. */
    String getName();
}
