package com.sunrise.dentalclinic.controller;

import com.sunrise.dentalclinic.dto.request.SettlePaymentRequest;
import com.sunrise.dentalclinic.dto.response.BillResponse;
import com.sunrise.dentalclinic.service.BillService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** "Calculate and Print Bill" from the brief. */
@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @GetMapping("/appointment/{appointmentNumber}")
    public BillResponse generateOrFetch(@PathVariable String appointmentNumber) {
        return billService.generateOrFetch(appointmentNumber);
    }

    @PostMapping("/{billNumber}/settle")
    public BillResponse settle(@PathVariable String billNumber, @Valid @RequestBody SettlePaymentRequest request) {
        return billService.settlePayment(billNumber, request);
    }
}
