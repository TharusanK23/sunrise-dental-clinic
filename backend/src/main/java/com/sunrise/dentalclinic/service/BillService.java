package com.sunrise.dentalclinic.service;

import com.sunrise.dentalclinic.dto.request.SettlePaymentRequest;
import com.sunrise.dentalclinic.dto.response.BillResponse;

public interface BillService {
    BillResponse generateOrFetch(String appointmentNumber);
    BillResponse settlePayment(String billNumber, SettlePaymentRequest request);
}
