package com.sunrise.dentalclinic.pattern.strategy;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Applied when a returning patient has 5+ prior completed visits: rewards loyal patients with a percentage discount on the consultation fee. */
public class LoyaltyDiscountPricingStrategy extends AbstractPricingStrategy {

    private final double discountRate;

    public LoyaltyDiscountPricingStrategy(double taxRate, double discountRate) {
        super(taxRate);
        this.discountRate = discountRate;
    }

    @Override
    public PricingResult calculate(BigDecimal consultationFee, long patientVisitCount, LocalDate appointmentDate) {
        BigDecimal discount = consultationFee.multiply(BigDecimal.valueOf(discountRate));
        return buildResult(consultationFee, BigDecimal.ZERO, discount);
    }

    @Override
    public String getName() {
        return "LOYALTY_DISCOUNT";
    }
}
