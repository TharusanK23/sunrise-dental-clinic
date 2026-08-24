# Test Case Matrix — Sunrise Dental Clinic System

Legend for **Type**: `POS` positive · `NEG` negative · `BND` boundary ·
`VAL` validation · `API` API contract · `DB` database-level · `INT`
integration

All statuses below are **PASS**, captured against the automated suite (see
`testing/evidence/*.txt`, `Tests run: 25, Failures: 0, Errors: 0`) plus a
manual pass against the real MySQL/XAMPP instance per `docs/SETUP.md` §7.

## Authentication

| ID | Type | Description | Steps | Expected Result | Automated In | Status |
|---|---|---|---|---|---|---|
| AUTH-01 | POS | Valid staff login succeeds | POST `/api/auth/login` with `nadeesha`/`Nadeesha@123` | `200 OK`, user JSON + `dc_token` HttpOnly cookie set | `AppointmentFlowIntegrationTest.fullAppointmentAndBillingFlow` (via `loginAndGetCookie()` helper) | PASS |
| AUTH-02 | NEG | Wrong password rejected | POST `/api/auth/login` with `nadeesha`/`wrong-password` | `401 Unauthorized` | Manual (Postman "Login - Wrong Password") | PASS |
| AUTH-03 | NEG | Unknown username rejected | POST `/api/auth/login` with a non-existent username | `401 Unauthorized` (no user enumeration - same message as AUTH-02) | Manual (Postman "Login - Unknown User") | PASS |
| AUTH-04 | VAL | Blank username/password rejected | POST `/api/auth/login` with `{"username":"","password":""}` | `400 Bad Request` with `validationErrors.username`/`.password` | Manual (Postman) | PASS |
| AUTH-05 | NEG | Protected endpoint rejects an anonymous request | GET `/api/appointments` with no cookie | `401 Unauthorized` | `AppointmentFlowIntegrationTest.protectedEndpointRequiresAuthentication` | PASS |
| AUTH-06 | POS | Logout clears the session | POST `/api/auth/logout`, then GET `/api/auth/me` | `204` then `401` on the follow-up call | Manual (Postman sequence) | PASS |
| AUTH-07 | API | `ADMIN`-only endpoint rejects `STAFF` | Logged in as `nadeesha`, POST `/api/users` | `403 Forbidden` | Manual (Postman, role check) | PASS |

## Register New Appointment

| ID | Type | Description | Steps | Expected Result | Automated In | Status |
|---|---|---|---|---|---|---|
| APT-01 | POS | Register an appointment for an existing patient | POST `/api/appointments` with `patientId` + valid dentist/treatment/date-time | `201 Created`, `appointmentNumber` starts with `APT-`, status `CONFIRMED` | `AppointmentServiceImplTest.registersAppointmentSuccessfully`, `AppointmentFlowIntegrationTest.fullAppointmentAndBillingFlow` | PASS |
| APT-02 | NEG | Reject booking a dentist who is on leave or inactive | Register with a `dentistId` whose status is `ON_LEAVE`/`INACTIVE` | `409 Conflict`, message names the dentist | `AppointmentServiceImplTest.rejectsUnavailableDentist` | PASS |
| APT-03 | NEG | Reject a double-booking for the same dentist at the same date/time | Register two appointments for the same dentist with the identical date and time | Second request: `409 Conflict` | `AppointmentServiceImplTest.rejectsDoubleBooking`, `AppointmentFlowIntegrationTest.fullAppointmentAndBillingFlow` | PASS |
| APT-04 | NEG | Reject a request with neither an existing nor a new patient | Omit `patientId` **and** `patientFullName`/`address`/`contact` | `409 Conflict` with a clear "patientId or name/address/contact" message | `AppointmentServiceImplTest.rejectsMissingPatientDetails` | PASS |
| APT-05 | VAL | Reject an appointment date in the past | Build via `AppointmentBuilder` with a past `appointmentDate` | `IllegalStateException` → `400 Bad Request` at the API boundary | `AppointmentBuilderTest.rejectsPastDate` | PASS |
| APT-06 | BND | Same-day appointment requires a time not already passed | `appointmentDate == today`, `appointmentTime` earlier than now | Rejected | `AppointmentBuilderTest.rejectsPastTimeForSameDayBooking` | PASS |
| APT-07 | VAL | Reject missing mandatory fields | Omit `dentist` from the builder | `IllegalStateException` | `AppointmentBuilderTest.rejectsMissingMandatoryField` | PASS |
| APT-08 | VAL | Reject an invalid contact number format | POST with `patientContactNumber: "abc"` | `400 Bad Request`, `validationErrors.patientContactNumber` | Manual (Postman) | PASS |
| APT-09 | NEG | Reject an unknown dentist id | POST with a `dentistId` that does not exist | `404 Not Found` | `AppointmentServiceImplTest.rejectsUnknownDentist` | PASS |
| APT-10 | POS | Successful registration produces a unique, correctly-formatted number | Inspect `appointmentNumber` in the `201` response | Matches `APT-\d{4}-\d{6}` and is unique per call | `AppointmentBuilderTest.buildsValidAppointment` | PASS |
| APT-11 | POS | Register an appointment for a *new* patient inline | POST with `patientFullName`/`patientAddress`/`patientContactNumber`, no `patientId` | `201 Created`, response's `patient` matches the newly created record | Manual (Postman "Register - New Patient") | PASS |

## Search / Display Appointment

| ID | Type | Description | Steps | Expected Result | Automated In | Status |
|---|---|---|---|---|---|---|
| SRCH-01 | POS | Find an appointment by its number | GET `/api/appointments/{validNumber}` | `200 OK` with full patient + dentist + treatment details | `AppointmentFlowIntegrationTest.fullAppointmentAndBillingFlow` | PASS |
| SRCH-02 | NEG | Unknown appointment number | GET `/api/appointments/APT-2026-999999` | `404 Not Found` | Manual (Postman) | PASS |

## Cancel / Update Appointment

| ID | Type | Description | Steps | Expected Result | Automated In | Status |
|---|---|---|---|---|---|---|
| CANC-01 | POS | Cancelling a CONFIRMED appointment succeeds | POST `/api/appointments/{number}/cancel` | Appointment → `CANCELLED` | `AppointmentServiceImplTest.cancelsAppointment` | PASS |
| CANC-02 | NEG | Cannot cancel a completed appointment | POST cancel on a `COMPLETED` appointment | `409 Conflict` | `AppointmentServiceImplTest.rejectsCancellingCompletedAppointment` | PASS |

## Calculate and Print Bill

| ID | Type | Description | Steps | Expected Result | Automated In | Status |
|---|---|---|---|---|---|---|
| BILL-01 | POS | Standard weekday pricing | Weekday appointment, Rs.2,500 consultation fee, new patient | subtotal 2,500; tax 200 (8%); total 2,700; `strategyName = STANDARD` | `PricingContextTest$StandardPricing.calculatesStandardPriceForWeekday`, `BillFactoryTest.createsBillForNewPatientOnWeekday` | PASS |
| BILL-02 | BND | Exactly 4 prior visits does not yet qualify for the loyalty discount | 4 prior COMPLETED visits, weekday | `strategyName = STANDARD` | `PricingContextTest$StandardPricing.fourVisitsDoesNotQualifyForDiscount` | PASS |
| BILL-03 | POS | Weekend surcharge applied on a Saturday appointment | Saturday appointment | 15% surcharge on subtotal, `strategyName = WEEKEND_SURCHARGE` | `PricingContextTest$WeekendPricing.appliesWeekendSurchargeOnSaturday` | PASS |
| BILL-04 | POS | Weekend surcharge also applies on a Sunday appointment | Sunday appointment | `strategyName = WEEKEND_SURCHARGE` | `PricingContextTest$WeekendPricing.appliesWeekendSurchargeOnSunday` | PASS |
| BILL-05 | POS | Loyalty discount (5+ completed visits) applied | 5 prior COMPLETED visits | 10% discount on subtotal, `strategyName = LOYALTY_DISCOUNT` | `PricingContextTest$LoyaltyPricing.appliesLoyaltyDiscountAndOverridesWeekendSurcharge` | PASS |
| BILL-06 | BND | Loyalty discount takes priority over a weekend surcharge | 5+ prior visits *and* a Saturday appointment | Discount applied, **no** surcharge (mutually exclusive by design) | `PricingContextTest$LoyaltyPricing.appliesLoyaltyDiscountAndOverridesWeekendSurcharge` | PASS |
| BILL-07 | BND | Well beyond the threshold (10 visits) still applies the same discount rate | 10 prior completed visits | `strategyName = LOYALTY_DISCOUNT`, discount unchanged at 10% | `PricingContextTest$LoyaltyPricing.manyVisitsStillAppliesLoyaltyDiscount` | PASS |
| BILL-08 | POS | Bill number is correctly derived from the appointment number | `create(appointment)` with `appointmentNumber = APT-2026-000042` | `billNumber = INV-2026-000042` | `BillFactoryTest.createsBillWithLoyaltyDiscountForReturningPatient` | PASS |
| BILL-09 | POS | Fetching an already-generated bill returns the same figures | Call GET `/api/bills/appointment/{number}` twice | Both responses identical (bill persisted, not recomputed) | Manual (Postman, repeat call) | PASS |
| BILL-10 | POS | Settling a bill marks it paid | POST `/api/bills/{billNumber}/settle` with a payment method | `paymentStatus = PAID`, `paymentMethod` recorded | Manual (Postman) | PASS |
| BILL-11 | INT | Full price includes 8% tax end-to-end via the live API | Register + generate bill via `AppointmentFlowIntegrationTest` | `totalAmount = 2700.00` for a standard weekday Rs.2,500 consultation | `AppointmentFlowIntegrationTest.fullAppointmentAndBillingFlow` | PASS |

## Dentists, Treatment Types & Patients

| ID | Type | Description | Steps | Expected Result | Automated In | Status |
|---|---|---|---|---|---|---|
| DEN-01 | POS | Available-dentist search excludes conflicting appointments | GET `/api/dentists/available?appointmentDate=...&appointmentTime=...` after a booking exists for that exact slot | The booked dentist is excluded from results | Manual (Postman + UI walkthrough) | PASS |
| DEN-02 | VAL | Reject a treatment type with a non-positive consultation fee | POST `/api/treatment-types` with `consultationFee: 0` | `400 Bad Request`, `validationErrors.consultationFee` | Manual (Postman) | PASS |
| DEN-03 | NEG | Reject a duplicate treatment name | POST `/api/treatment-types` with a name already in use | `409 Conflict` (unique constraint) | Manual (Postman) | PASS |
| DEN-04 | API | `ADMIN`-only delete on dentists/treatment types rejects `STAFF` | Logged in as `nadeesha`, DELETE `/api/dentists/1` | `403 Forbidden` | Manual (Postman, role check) | PASS |

## Database-level tests (run directly against MySQL)

| ID | Type | Description | Steps | Expected Result | Status |
|---|---|---|---|---|---|
| DB-01 | DB | `trg_prevent_double_booking` blocks a raw SQL double-insert | `INSERT INTO appointments` directly with the same dentist/date/time as an existing active appointment | Statement fails with `SIGNAL SQLSTATE '45000'` and the custom message | PASS |
| DB-02 | DB | `trg_sync_dentist_status` logs an audit row on status change | `UPDATE appointments SET status='COMPLETED' WHERE ...` | A corresponding `AUDIT` row appears in `notification_logs` | PASS |
| DB-03 | DB | `sp_daily_revenue_report` returns correct aggregates | `CALL sp_daily_revenue_report('2026-01-01','2026-12-31')` after several bills exist | One row per date, `total_revenue` = sum of that day's `bills.total_amount` | PASS |
| DB-04 | DB | `fn_is_weekend_appointment` correctly flags Saturday/Sunday | `SELECT fn_is_weekend_appointment('2026-08-29')` (a Saturday) | Returns `1` | PASS |
| DB-05 | DB | `vw_dentist_utilization` counts only non-cancelled appointments | Cancel one appointment for a dentist who also has a confirmed one | `times_booked` reflects only the confirmed appointment | PASS |
| DB-06 | DB | Foreign-key/CHECK constraints reject invalid rows | `INSERT INTO dentists (..., status) VALUES (..., 'RETIRED')` | Rejected by `chk_dentists_status` | PASS |

## Staff Accounts (Admin: Edit Staff Details)

| ID | Type | Description | Steps | Expected Result | Automated In | Status |
|---|---|---|---|---|---|---|
| USER-01 | POS | Admin edits a staff member's full name and email | `update(1L, new UpdateUserRequest(...))`, logged in as `admin` | Response reflects the new name/email; `username`, `role` and password hash unchanged | `UserServiceImplTest.updatesStaffNameAndEmailOnly` | PASS |
| USER-02 | NEG | Editing an unknown staff id returns 404 | `update(99L, ...)` | `ResourceNotFoundException` → `404 Not Found` | `UserServiceImplTest.rejectsUpdateForUnknownUser` | PASS |
| USER-03 | VAL | Blank full name is rejected | PATCH with `{"fullName": "", "email": "x@sunrise.lk"}` | `400 Bad Request`, `validationErrors.fullName` | Manual (Postman + live curl, `docs/SETUP.md`) | PASS |
| USER-04 | VAL | Invalid email format is rejected | PATCH with `{"fullName": "X", "email": "not-an-email"}` | `400 Bad Request`, `validationErrors.email` | Manual (Postman) | PASS |
| USER-05 | API | `STAFF` role cannot edit staff accounts (Admin-only) | Logged in as `nadeesha` (STAFF), PATCH `/api/users/1` | `403 Forbidden` | Manual (Postman, role check; also verified live via curl) | PASS |
| USER-06 | INT | End-to-end edit via the UI | Staff Accounts page → "Edit" on a row → change name/email → "Save Changes" | Modal closes, table refreshes with the new values, no page reload needed | Manual (screenshots `testing/screenshots/`) | PASS |
| USER-07 | POS | Create a new staff account with a BCrypt-hashed password | `create(new CreateUserRequest(...))` | `passwordEncoder.encode` invoked, plaintext password never stored | `UserServiceImplTest.createsNewStaffAccount` | PASS |
| USER-08 | NEG | Reject a duplicate username on account creation | `create(...)` with an already-taken username | `DuplicateResourceException` → `409 Conflict` | `UserServiceImplTest.rejectsDuplicateUsername` | PASS |

## Cross-cutting API contract tests

| ID | Type | Description | Steps | Expected Result | Automated In | Status |
|---|---|---|---|---|---|---|
| API-01 | API | Validation errors return a consistent, field-mapped shape | Any endpoint with `@Valid` violations | `400` body matches `ApiErrorResponse` with a populated `validationErrors` map | All `@Valid`-annotated endpoints (`GlobalExceptionHandler`) | PASS |
| API-02 | API | Unhandled resource-not-found returns `404`, not `500` | GET any `/api/{resource}/{unknownId}` | `404 Not Found` with a descriptive message | `AppointmentServiceImplTest.rejectsUnknownDentist` | PASS |
| API-03 | API | Health endpoint is unauthenticated | GET `/api/health` with no cookie | `200 OK` | Manual (curl, `docs/SETUP.md` §6.2) | PASS |

---

**Total automated test methods: 25** (run via `./mvnw test`; see
`testing/evidence/` for the captured passing output and `TEST_PLAN.md` §4
for how each level of this matrix maps to a tool).
