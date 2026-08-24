package com.sunrise.dentalclinic.pattern.strategy;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Applied when the appointment date falls on Saturday or Sunday: adds a weekend-slot surcharge on top of the consultation fee. */
public class WeekendSurchargePricingStrategy extends AbstractPricingStrategy {

    private final double surchargeRate;

    public WeekendSurchargePricingStrategy(double taxRate, double surchargeRate) {
        super(taxRate);
        this.surchargeRate = surchargeRate;
    }

    @Override
    public PricingResult calculate(BigDecimal consultationFee, long patientVisitCount, LocalDate appointmentDate) {
        BigDecimal surcharge = consultationFee.multiply(BigDecimal.valueOf(surchargeRate));
        return buildResult(consultationFee, surcharge, BigDecimal.ZERO);
    }

    @Override
    public String getName() {
        return "WEEKEND_SURCHARGE";
    }
}
