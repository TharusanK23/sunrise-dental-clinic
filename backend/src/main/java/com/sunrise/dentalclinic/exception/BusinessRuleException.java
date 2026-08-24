package com.sunrise.dentalclinic.exception;

/** Raised when a request is well-formed but violates a business rule (e.g. double-booking a dentist, an unavailable dentist). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
