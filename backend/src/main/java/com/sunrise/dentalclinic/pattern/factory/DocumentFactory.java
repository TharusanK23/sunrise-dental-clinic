package com.sunrise.dentalclinic.pattern.factory;

/**
 * Factory Method pattern: a common creation contract implemented by every
 * concrete document factory in the system ({@code BillFactory} today; a
 * future {@code ReportFactory} for revenue/utilisation reports follows the
 * same shape). Callers depend only on this interface, so a new document
 * type can be introduced without touching existing client code.
 *
 * @param <T> the type of domain object produced (e.g. {@code Bill})
 * @param <S> the source object the document is generated from (e.g. {@code Appointment})
 */
public interface DocumentFactory<T, S> {
    T create(S source);
}
