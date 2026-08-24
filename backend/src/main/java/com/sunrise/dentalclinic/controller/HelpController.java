package com.sunrise.dentalclinic.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * "Help Section" from the brief: step-by-step instructions for new staff,
 * served as data so the frontend's help.html page can render it (kept in the
 * backend, rather than hard-coded only in HTML, so it can be updated
 * centrally without touching the client).
 */
@RestController
public class HelpController {

    @GetMapping("/api/help")
    public List<Map<String, String>> help() {
        return List.of(
                Map.of("step", "1", "title", "Log in",
                        "description", "Enter the username and password issued by your administrator on the Login screen and click Sign In. Only authorised staff accounts can access the system."),
                Map.of("step", "2", "title", "Register a new appointment",
                        "description", "Open 'New Appointment', search for an existing patient or fill in the new patient's name, address and contact number, choose a dentist and treatment type, pick the appointment date and time, then click Save. A unique appointment number is generated automatically."),
                Map.of("step", "3", "title", "Find an appointment",
                        "description", "Open 'Search Appointment' and type the appointment number (e.g. APT-2026-000123) to view the complete patient and appointment details."),
                Map.of("step", "4", "title", "Calculate and print the bill",
                        "description", "From an appointment's details page click 'Generate Bill'. The total is calculated automatically from the treatment type's consultation fee, any weekend surcharge or loyalty discount, and tax. Click 'Print Receipt' to print or save it as a PDF."),
                Map.of("step", "5", "title", "View reports",
                        "description", "Admins and staff can open 'Reports' to see today's dashboard, daily revenue, and dentist utilisation to support day-to-day decisions."),
                Map.of("step", "6", "title", "Exit the system",
                        "description", "Click 'Logout' in the top-right menu at any time to safely end your session; the system securely clears your session cookie.")
        );
    }
}
