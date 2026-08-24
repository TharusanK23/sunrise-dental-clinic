package com.sunrise.dentalclinic.pattern.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Shared rounding/tax behaviour so concrete strategies only need to supply subtotal, surcharge and discount. */
abstract class AbstractPricingStrategy implements PricingStrategy {

    protected final double taxRate;

    protected AbstractPricingStrategy(double taxRate) {
        this.taxRate = taxRate;
    }

    protected PricingResult buildResult(BigDecimal subtotal, BigDecimal surcharge, BigDecimal discount) {
        BigDecimal taxableAmount = subtotal.add(surcharge).subtract(discount);
        BigDecimal tax = taxableAmount.multiply(BigDecimal.valueOf(taxRate)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = taxableAmount.add(tax).setScale(2, RoundingMode.HALF_UP);
        return new PricingResult(
                subtotal.setScale(2, RoundingMode.HALF_UP),
                surcharge.setScale(2, RoundingMode.HALF_UP),
                discount.setScale(2, RoundingMode.HALF_UP),
                tax,
                total,
                getName()
        );
    }
}
