package com.sunrise.dentalclinic.pattern.strategy;

import java.math.BigDecimal;

/**
 * Immutable outcome of applying a {@link PricingStrategy}: the components
 * that make up a bill before persistence (subtotal, surcharge, discount,
 * tax, grand total) plus the name of the strategy that produced it, for
 * traceability on the printed receipt.
 */
public record PricingResult(
        BigDecimal subtotal,
        BigDecimal surchargeAmount,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String strategyName
) {
}
