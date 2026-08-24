# Test Plan — Sunrise Dental Clinic System

**Module:** CIS6003 Advanced Programming | **Task:** C (Testing, 20 marks)

## 1. Scope

This plan covers testing of the backend REST API (`backend/`), its
integration with MySQL, and the end-to-end behaviour exercised through the
frontend client. It excludes purely cosmetic/CSS testing and third-party
libraries (Spring Framework, Bootstrap) that are assumed correct.

## 2. Objectives

1. Verify every functionality in the assignment brief works correctly:
   login, register a new appointment, search/display an appointment,
   calculate and print a bill, the help section, and logout ("exit
   system").
2. Verify the business rules that are *not* stated explicitly in the brief
   but are essential to a credible clinic system: no double-booking of a
   dentist, correct pricing under three different strategies, and correct
   role-based access.
3. Verify the system's persistence layer (MySQL, including the triggers,
   stored procedure and views) behaves correctly, not just the Java code
   in isolation.
4. Produce durable, automated evidence (a JUnit suite that can be re-run by
   any grader with one command) rather than only a manual click-through
   record.

## 3. Test-Driven Development (TDD) — rationale and how it was applied

TDD was applied specifically to the **pricing/billing logic**
(`pattern.strategy` package), because it is the most business-critical,
purely computational part of the system — a bug here directly produces a
wrong bill, which is the single output a patient scrutinises most closely.

**Why this component and not the whole codebase:** TDD pays off most where
(a) requirements can be expressed as concrete input→output examples before
any code exists, and (b) the logic is easy to isolate from infrastructure
(no DB, no HTTP). The pricing rules — *"a weekday consultation has no
surcharge and an 8% tax"*, *"a Saturday appointment adds a 15% surcharge"*,
*"5+ completed prior visits gets a 10% loyalty discount, which overrides
the weekend surcharge"* — are exactly this kind of rule. Applying TDD to
CRUD-shaped code (simple repository pass-throughs) would add process
overhead without added confidence, so those layers were instead covered
with tests written immediately alongside the implementation
(test-immediately, not test-first) and with one full integration test.

**The red→green→refactor cycle actually followed for this component:**

1. **Red.** `PricingContextTest`
   (`backend/src/test/java/.../pattern/strategy/PricingContextTest.java`)
   was written against a `PricingContext` class and three strategy classes
   that **did not yet exist**. The test suite failed to compile — the
   first "red".
2. **Green.** `PricingStrategy`, `StandardPricingStrategy`,
   `WeekendSurchargePricingStrategy`, `LoyaltyDiscountPricingStrategy` and
   `PricingContext` were implemented one at a time, re-running the
   relevant `@Nested` test group after each, until all assertions passed
   with the simplest logic that satisfied them.
3. **Refactor.** Once all three strategies were green, the duplicated
   "apply tax and round to 2 d.p." logic that had been copy-pasted into
   each `calculate()` method was pulled up into
   `AbstractPricingStrategy.buildResult()`. The test suite was re-run
   **unchanged** and still passed, which is exactly the guarantee TDD is
   meant to provide during a refactor.

The boundary cases in that same test class (exactly 4 vs. exactly 5 prior
completed visits either side of the loyalty-discount threshold, a
Friday/Saturday/Sunday appointment date, and a loyalty discount overriding
a weekend surcharge) were written as part of the same red→green cycle,
before the corresponding precedence logic was finalised in
`PricingContext.price()`.

## 4. Test levels & tools

| Level | Tool | What it proves |
|---|---|---|
| Unit | JUnit 5 + AssertJ | Pure logic in isolation (pricing math, `AppointmentBuilder` validation) |
| Unit (with collaborators) | JUnit 5 + Mockito | Service-layer branching logic with repositories/publishers mocked out (`AppointmentServiceImplTest`, `BillFactoryTest`, `UserServiceImplTest`) |
| Integration | Spring Boot Test (`@SpringBootTest`) + MockMvc + H2 in-memory DB | The real Spring context, the real Spring Security filter chain, and real JPA/Hibernate SQL generation, wired together |
| API (manual/exploratory) | Postman collection, curl (see `docs/SETUP.md` §6) | Contract-level verification against a **real** MySQL instance via XAMPP |
| Database | MySQL CLI against `database/schema.sql` | Triggers, stored procedure, views, and constraints behave as designed |

Automated tests run against **H2** (fast, no external service needed, safe
to run in CI — see `.github/workflows/ci.yml`); manual/API testing runs
against the **real MySQL** instance to validate the parts H2 cannot (the
MySQL-specific trigger/procedure/views), per the note in `docs/SETUP.md`
§9.6.

## 5. Test data

- **Automated tests:** each test class seeds its own minimal, deterministic
  data in `@BeforeEach` (e.g. one dentist, one treatment type, one staff
  user) so tests are independent and repeatable in any order. The
  integration test class is annotated `@Transactional` so each test method
  runs and rolls back independently within the shared H2 in-memory schema.
- **Manual/API testing:** `database/schema.sql` seeds 2 staff accounts, 5
  dentists, 6 treatment types, and 3 patients — see `docs/SETUP.md` §2.4.
- **Boundary test data** is chosen deliberately at the edges of each
  business rule: exactly 4 vs. exactly 5 prior completed visits (either
  side of the loyalty-discount threshold), a Friday/Saturday/Sunday vs. a
  Tuesday appointment date, and a same-day appointment with an invalid
  (already-passed) time.

## 6. Entry / exit criteria

**Entry:** the feature under test compiles and its dependencies (a running
MySQL for manual tests) are available per `docs/SETUP.md`.

**Exit:** `./mvnw test` reports zero failures and zero errors across all
test classes (see `testing/evidence/` for a captured passing run at 26/26
tests), and every row in `TEST_CASES.md` has a `PASS` status.

## 7. Test automation

All JUnit tests are runnable with a single command (`./mvnw test`, see
`docs/SETUP.md` §8) and are wired into GitHub Actions CI
(`.github/workflows/ci.yml`) so they run automatically on every push to
`dev`/`release` — see `docs/GIT_WORKFLOW.md`.

## 8. Traceability

`TEST_CASES.md` includes an "Automated in" column mapping every test case
back to the exact test class/method (or Postman request) that proves it,
and back to the brief requirement or design decision it verifies.

## 9. Risks & mitigations

| Risk | Mitigation |
|---|---|
| H2 test DB behaves subtly differently from MySQL (dialect quirks) | The H2 dialect is explicitly configured at the same property path (`spring.jpa.properties.hibernate.dialect`) as production, avoiding a silent-override bug encountered in the companion vehicle-reservation project; no MySQL-reserved-word column names are used |
| Trigger/procedure/view logic (MySQL-only) is invisible to the H2-based automated suite | Covered separately by the manual database-level test cases in §"Database Tests" of `TEST_CASES.md`, executed directly via the MySQL CLI |
| Time-sensitive tests (e.g. "appointment date in the future") becoming stale | Dates in builder/integration tests are computed relative to `LocalDate.now()`, not hard-coded |
| Shared H2 schema across `@Transactional` integration test methods causing seed-data collisions | Encountered directly during development (a second test method's `@BeforeEach` re-inserted the same seeded username and violated a unique constraint); fixed by annotating the integration test class `@Transactional` so each test method's changes roll back automatically |
