package com.sunrise.dentalclinic.pattern.strategy;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Default strategy: subtotal = consultation fee, no surcharge, no discount, plus standard tax. */
public class StandardPricingStrategy extends AbstractPricingStrategy {

    public StandardPricingStrategy(double taxRate) {
        super(taxRate);
    }

    @Override
    public PricingResult calculate(BigDecimal consultationFee, long patientVisitCount, LocalDate appointmentDate) {
        return buildResult(consultationFee, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    @Override
    public String getName() {
        return "STANDARD";
    }
}
