package com.sunrise.dentalclinic.pattern.strategy;

import com.sunrise.dentalclinic.config.BusinessProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Strategy pattern "context": chooses the correct {@link PricingStrategy} for
 * a given appointment and delegates the calculation to it. Precedence rule
 * (documented in the assignment report): a loyalty discount for a returning
 * patient takes priority over a weekend surcharge, which in turn takes
 * priority over standard pricing.
 */
@Component
public class PricingContext {

    private final BusinessProperties props;

    public PricingContext(BusinessProperties props) {
        this.props = props;
    }

    public PricingResult price(BigDecimal consultationFee, long patientVisitCount, LocalDate appointmentDate) {
        PricingStrategy strategy = resolveStrategy(patientVisitCount, appointmentDate);
        return strategy.calculate(consultationFee, patientVisitCount, appointmentDate);
    }

    private PricingStrategy resolveStrategy(long patientVisitCount, LocalDate appointmentDate) {
        if (patientVisitCount >= props.getLoyaltyDiscountVisitCount()) {
            return new LoyaltyDiscountPricingStrategy(props.getTaxRate(), props.getLoyaltyDiscountRate());
        }
        DayOfWeek day = appointmentDate.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return new WeekendSurchargePricingStrategy(props.getTaxRate(), props.getWeekendSurchargeRate());
        }
        return new StandardPricingStrategy(props.getTaxRate());
    }
}
