package com.sunrise.dentalclinic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalised business rules (tax rate, surcharges, loyalty-discount
 * threshold) bound from {@code application.yml} (prefix {@code app.business})
 * so pricing policy can change without recompiling the pricing
 * {@link com.sunrise.dentalclinic.pattern.strategy.PricingStrategy} implementations.
 */
@ConfigurationProperties(prefix = "app.business")
public class BusinessProperties {

    private double taxRate;
    private double weekendSurchargeRate;
    private int loyaltyDiscountVisitCount;
    private double loyaltyDiscountRate;
    private String currencySymbol;

    public double getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(double taxRate) {
        this.taxRate = taxRate;
    }

    public double getWeekendSurchargeRate() {
        return weekendSurchargeRate;
    }

    public void setWeekendSurchargeRate(double weekendSurchargeRate) {
        this.weekendSurchargeRate = weekendSurchargeRate;
    }

    public int getLoyaltyDiscountVisitCount() {
        return loyaltyDiscountVisitCount;
    }

    public void setLoyaltyDiscountVisitCount(int loyaltyDiscountVisitCount) {
        this.loyaltyDiscountVisitCount = loyaltyDiscountVisitCount;
    }

    public double getLoyaltyDiscountRate() {
        return loyaltyDiscountRate;
    }

    public void setLoyaltyDiscountRate(double loyaltyDiscountRate) {
        this.loyaltyDiscountRate = loyaltyDiscountRate;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }
}
