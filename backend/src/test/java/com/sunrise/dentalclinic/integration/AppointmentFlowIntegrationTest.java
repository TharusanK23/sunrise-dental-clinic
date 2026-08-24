package com.sunrise.dentalclinic.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunrise.dentalclinic.entity.*;
import com.sunrise.dentalclinic.repository.DentistRepository;
import com.sunrise.dentalclinic.repository.PatientRepository;
import com.sunrise.dentalclinic.repository.TreatmentTypeRepository;
import com.sunrise.dentalclinic.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end flow test (login -&gt; register appointment -&gt; conflict rejected
 * -&gt; generate bill) against a real Spring context wired to an in-memory H2
 * database, exercising the full stack: Spring Security's JWT-cookie
 * authentication, the Builder/Observer/Factory/Strategy/Singleton design
 * patterns, and the JPA repository layer together - not just a single
 * class in isolation, unlike the other test classes in this suite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AppointmentFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DentistRepository dentistRepository;
    @Autowired private TreatmentTypeRepository treatmentTypeRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Dentist dentist;
    private TreatmentType treatmentType;
    private Patient patient;

    @BeforeEach
    void seed() {
        userRepository.save(User.builder()
                .username("nadeesha").password(passwordEncoder.encode("Nadeesha@123"))
                .fullName("Nadeesha Fernando").email("nadeesha@sunrise.lk").role(Role.STAFF).enabled(true).build());

        dentist = dentistRepository.save(Dentist.builder()
                .fullName("Dr. Perera").specialization("General Dentistry")
                .contactNumber("0711111111").status(DentistStatus.AVAILABLE).build());

        treatmentType = treatmentTypeRepository.save(TreatmentType.builder()
                .treatmentName("Scaling").consultationFee(BigDecimal.valueOf(2500))
                .description("Dental scaling and polishing").build());

        patient = patientRepository.save(Patient.builder()
                .fullName("Kasun Silva").address("Colombo").contactNumber("0772223344").build());
    }

    private Cookie loginAndGetCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("username", "nadeesha", "password", "Nadeesha@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username", is("nadeesha")))
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("dc_token");
        assertThat(cookie).isNotNull();
        return cookie;
    }

    @Test
    @DisplayName("An unauthenticated request to a protected endpoint is rejected with 401")
    void protectedEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/appointments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Full flow: login, register an appointment, generate its bill, then reject a conflicting double-booking")
    void fullAppointmentAndBillingFlow() throws Exception {
        Cookie authCookie = loginAndGetCookie();

        LocalDate appointmentDate = LocalDate.now().plusDays(1);
        LocalTime appointmentTime = LocalTime.of(9, 30);

        Map<String, Object> registerBody = Map.of(
                "patientId", patient.getId(),
                "dentistId", dentist.getId(),
                "treatmentTypeId", treatmentType.getId(),
                "appointmentDate", appointmentDate.format(DateTimeFormatter.ISO_DATE),
                "appointmentTime", appointmentTime.toString(),
                "notes", "Routine checkup"
        );

        MvcResult registerResult = mockMvc.perform(post("/api/appointments")
                        .cookie(authCookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appointmentNumber").exists())
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andReturn();

        String appointmentNumber = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("appointmentNumber").asText();

        // Same dentist, same date/time -> must be rejected (409) both by the service check and trg_prevent_double_booking in production.
        mockMvc.perform(post("/api/appointments")
                        .cookie(authCookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/bills/appointment/{apptNo}", appointmentNumber).cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billNumber").exists())
                .andExpect(jsonPath("$.pricingStrategy", is("STANDARD")))
                .andExpect(jsonPath("$.totalAmount", is(2700.00)))
                .andExpect(jsonPath("$.paymentStatus", is("UNPAID")));
    }
}
