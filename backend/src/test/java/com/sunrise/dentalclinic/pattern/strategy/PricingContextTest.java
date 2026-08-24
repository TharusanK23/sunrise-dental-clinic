package com.sunrise.dentalclinic.pattern.strategy;

import com.sunrise.dentalclinic.config.BusinessProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD note (see docs/ASSIGNMENT_REPORT.md, "Test-Driven Development" section):
 * this test class was written BEFORE {@link PricingContext} and its three
 * concrete strategies existed - the first run failed to compile (red), the
 * strategy classes were then implemented to the shape these tests demanded
 * (green), and the tax/rounding logic was subsequently pulled up into
 * {@link AbstractPricingStrategy} once all three strategies were passing
 * (refactor), without changing this file.
 */
class PricingContextTest {

    private PricingContext pricingContext;

    @BeforeEach
    void setUp() {
        BusinessProperties props = new BusinessProperties();
        props.setTaxRate(0.08);
        props.setWeekendSurchargeRate(0.15);
        props.setLoyaltyDiscountVisitCount(5);
        props.setLoyaltyDiscountRate(0.10);
        props.setCurrencySymbol("Rs.");
        pricingContext = new PricingContext(props);
    }

    @Nested
    @DisplayName("Standard weekday pricing")
    class StandardPricing {

        @Test
        @DisplayName("A weekday appointment for a new patient has no surcharge or discount, plus 8% tax")
        void calculatesStandardPriceForWeekday() {
            // Tuesday
            LocalDate weekday = LocalDate.of(2026, 8, 25);
            PricingResult result = pricingContext.price(BigDecimal.valueOf(2500), 0, weekday);

            assertThat(result.subtotal()).isEqualByComparingTo("2500.00");
            assertThat(result.surchargeAmount()).isEqualByComparingTo("0.00");
            assertThat(result.discountAmount()).isEqualByComparingTo("0.00");
            assertThat(result.taxAmount()).isEqualByComparingTo("200.00");
            assertThat(result.totalAmount()).isEqualByComparingTo("2700.00");
            assertThat(result.strategyName()).isEqualTo("STANDARD");
        }

        @Test
        @DisplayName("Boundary: a patient with exactly 4 prior visits does not yet qualify for the loyalty discount")
        void fourVisitsDoesNotQualifyForDiscount() {
            LocalDate weekday = LocalDate.of(2026, 8, 25);
            PricingResult result = pricingContext.price(BigDecimal.valueOf(2500), 4, weekday);

            assertThat(result.strategyName()).isEqualTo("STANDARD");
        }
    }

    @Nested
    @DisplayName("Weekend surcharge pricing")
    class WeekendPricing {

        @Test
        @DisplayName("An appointment on a Saturday applies a 15% weekend surcharge")
        void appliesWeekendSurchargeOnSaturday() {
            LocalDate saturday = LocalDate.of(2026, 8, 29);
            PricingResult result = pricingContext.price(BigDecimal.valueOf(2500), 0, saturday);

            assertThat(result.subtotal()).isEqualByComparingTo("2500.00");
            assertThat(result.surchargeAmount()).isEqualByComparingTo("375.00");
            assertThat(result.discountAmount()).isEqualByComparingTo("0.00");
            assertThat(result.strategyName()).isEqualTo("WEEKEND_SURCHARGE");
        }

        @Test
        @DisplayName("An appointment on a Sunday also applies the weekend surcharge")
        void appliesWeekendSurchargeOnSunday() {
            LocalDate sunday = LocalDate.of(2026, 8, 30);
            PricingResult result = pricingContext.price(BigDecimal.valueOf(2500), 0, sunday);

            assertThat(result.strategyName()).isEqualTo("WEEKEND_SURCHARGE");
        }
    }

    @Nested
    @DisplayName("Loyalty discount pricing")
    class LoyaltyPricing {

        @Test
        @DisplayName("A returning patient with 5+ prior completed visits gets a 10% discount, taking priority over any weekend surcharge")
        void appliesLoyaltyDiscountAndOverridesWeekendSurcharge() {
            LocalDate saturday = LocalDate.of(2026, 8, 29); // would otherwise trigger a weekend surcharge
            PricingResult result = pricingContext.price(BigDecimal.valueOf(2500), 5, saturday);

            assertThat(result.subtotal()).isEqualByComparingTo("2500.00");
            assertThat(result.discountAmount()).isEqualByComparingTo("250.00");
            assertThat(result.surchargeAmount()).isEqualByComparingTo("0.00");
            assertThat(result.strategyName()).isEqualTo("LOYALTY_DISCOUNT");
        }

        @Test
        @DisplayName("A patient with well beyond the threshold (10 visits) still gets exactly the same discount rate")
        void manyVisitsStillAppliesLoyaltyDiscount() {
            LocalDate weekday = LocalDate.of(2026, 8, 25);
            PricingResult result = pricingContext.price(BigDecimal.valueOf(2500), 10, weekday);

            assertThat(result.strategyName()).isEqualTo("LOYALTY_DISCOUNT");
            assertThat(result.discountAmount()).isEqualByComparingTo("250.00");
        }
    }
}
