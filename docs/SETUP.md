# Setup, Configuration & Testing Guide

Complete instructions for installing, configuring, running, and validating the
Sunrise Dental Clinic — Online Appointment & Patient Management System
entirely on **localhost**. This system was built and verified end-to-end on
Windows using XAMPP at `F:\Program\xampp`; adjust paths if your XAMPP is
installed elsewhere.

---

## 1. Prerequisites

| Requirement | Version used in development | Notes |
|---|---|---|
| Java (JDK) | 17 (LTS) | `java -version` |
| XAMPP | any recent version bundling MariaDB 10.4+ | Only **MySQL/MariaDB** and (optionally) **Apache** are used — PHP is not required by this project |
| A modern browser | Chrome/Edge/Firefox | For the client and for printing bills to PDF |
| Internet access | first run only | Maven Wrapper downloads Maven + all dependencies on the first build |
| Maven | *not required* | The project ships the **Maven Wrapper** (`mvnw` / `mvnw.cmd`), which downloads Maven automatically |

You do **not** need Node.js, PHP, or any global Maven install to run the
application itself — the backend is fully self-bootstrapping via the Maven
Wrapper, and the frontend is plain HTML/CSS/JS with Bootstrap vendored
locally (no build step, no CDN dependency). Node.js is only used as a
one-off documentation tool to render `docs/ASSIGNMENT_REPORT.md` to PDF/DOCX
and to render the Mermaid diagrams (see §11 and `diagrams/README.md`).

This project runs **alongside** the companion VehicleReservationSystem
project (different DB name, different backend port `8082` vs `8081`,
different frontend port `5501` vs `5500`) — both can be running at the same
time without conflict.

---

## 2. Database setup (MySQL via XAMPP)

### 2.1 Start MySQL

Open the **XAMPP Control Panel** and click **Start** next to *MySQL* (and
*Apache*, if you intend to serve the frontend through it — see §4). If you
prefer the command line:

```
"F:\Program\xampp\mysql_start.bat"
```

Verify it is listening:

```
"F:\Program\xampp\mysql\bin\mysql.exe" -u root -e "SELECT VERSION();"
```

You should see a MariaDB version string. If this fails, see
**Troubleshooting §9.1** below.

### 2.2 Import the schema

The complete schema (7 tables, 2 triggers, 1 stored procedure, 1 function, 2
views, and seed data) lives in **`database/schema.sql`**.

**Option A — phpMyAdmin (GUI):**
1. Open `http://localhost/phpmyadmin`
2. Click **Import** → **Choose File** → select `database/schema.sql` → **Go**

**Option B — command line (recommended, does not require phpMyAdmin/PHP to be working):**

```
"F:\Program\xampp\mysql\bin\mysql.exe" -u root < "database\schema.sql"
```

Both create the `sunrise_dental_clinic_db` database from scratch, so the
script is safe to re-run at any time to reset to a clean state.

### 2.3 Verify

```
"F:\Program\xampp\mysql\bin\mysql.exe" -u root sunrise_dental_clinic_db -e "SHOW TABLES; SELECT COUNT(*) FROM users;"
```

Expected tables: `appointments`, `bills`, `dentists`, `notification_logs`,
`patients`, `treatment_types`, `users`, plus views
`vw_dentist_utilization` and `vw_appointment_summary`. `users` should
contain 2 seeded accounts.

### 2.4 Seed accounts

| Username | Password | Role | Purpose |
|---|---|---|---|
| `admin` | `admin123` | ADMIN | Dentists, treatment types, staff accounts, all reports |
| `nadeesha` | `Nadeesha@123` | STAFF | Day-to-day appointment & billing operations |

Passwords are stored as BCrypt hashes (`database/schema.sql`); they are
never stored or transmitted in plain text.

---

## 3. Run the backend (Spring Boot API)

```
cd SunriseDentalClinic\backend
.\mvnw.cmd spring-boot:run
```

The first run downloads Maven itself plus all dependencies (needs internet)
and takes a minute or two; subsequent runs are fast. When ready you will
see:

```
Started DentalClinicApplication in X.XXX seconds
```

The API listens on **`http://localhost:8082`**. Confirm it is up:

```
curl http://localhost:8082/api/health
```

should return `{"status":"UP", ...}`.

Interactive API documentation (Swagger UI) is available at
**`http://localhost:8082/swagger-ui.html`**.

### 3.1 Configuration

All configuration lives in `backend/src/main/resources/application.yml`:

| Property | Default | Purpose |
|---|---|---|
| `server.port` | `8082` | API port (deliberately different from the vehicle project's `8081` so both can run together) |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/sunrise_dental_clinic_db` | XAMPP MySQL default port |
| `spring.datasource.username` / `password` | `root` / *(empty)* | XAMPP MySQL default credentials |
| `app.security.jwt.secret` | (demo key) | HMAC signing key for login tokens — **rotate before any real deployment** |
| `app.business.tax-rate`, `weekend-surcharge-rate`, `loyalty-discount-*` | 8% / 15% / 5 visits / 10% | Billing rules used by the Strategy pattern |
| `app.cors.allowed-origins` | `http://localhost,http://127.0.0.1,http://localhost:5501,...` | Add your frontend's exact origin here if it differs |

If your MySQL root user has a password, or XAMPP's MySQL runs on a
different port, edit `spring.datasource.url`/`username`/`password`
accordingly.

### 3.2 Stopping the backend

Press `Ctrl+C` in the terminal running `mvnw spring-boot:run`.

---

## 4. Run the frontend (client)

The client is pure static HTML/CSS/JS — no build step. Two ways to serve
it:

**Option A — XAMPP Apache (recommended, matches the brief's "use XAMPP" instruction):**

```
xcopy /E /I "frontend" "F:\Program\xampp\htdocs\sunrise-dental-clinic-client"
```

Start Apache in the XAMPP Control Panel, then open:

**`http://localhost/sunrise-dental-clinic-client/`**

**Option B — any static file server**, e.g. with Node:

```
npx http-server frontend -p 5501
```

then open `http://localhost:5501/`.

> Whichever port you use, the API's CORS configuration
> (`app.cors.allowed-origins` in `application.yml`) already allows
> `http://localhost` (port 80, XAMPP's default) and `http://localhost:5501`.
> Add any other port you use to that list and restart the backend.

### 4.1 Changing the API URL the frontend calls

`frontend/assets/js/api.js` defaults to `http://localhost:8082/api`.
Override it without editing the file by adding, before the other
`<script>` tags on any page, e.g.:
```html
<script>window.SDC_API_BASE = "http://localhost:8082/api";</script>
```

---

## 5. Accessing the application

| What | URL |
|---|---|
| Client (login page) | `http://localhost/sunrise-dental-clinic-client/` (or your chosen static server URL) |
| REST API base | `http://localhost:8082/api` |
| Swagger / OpenAPI UI | `http://localhost:8082/swagger-ui.html` |
| phpMyAdmin (optional) | `http://localhost/phpmyadmin` |

Log in with `admin`/`admin123` or `nadeesha`/`Nadeesha@123` (§2.4).

---

## 6. Testing the API directly

### 6.1 Postman

Import **`testing/postman/SunriseDentalClinic.postman_collection.json`**. It
is pre-configured with a `{{baseUrl}}` variable
(`http://localhost:8082/api`) and a login request that captures the session
cookie automatically for subsequent requests (Postman handles cookies
per-domain by default — just run the requests in order, or use "Run
collection").

### 6.2 curl

```bash
# Login (saves the session cookie to cookies.txt)
curl -c cookies.txt -H "Content-Type: application/json" \
     -d '{"username":"nadeesha","password":"Nadeesha@123"}' \
     http://localhost:8082/api/auth/login

# List dentists
curl -b cookies.txt http://localhost:8082/api/dentists

# Register an appointment
curl -b cookies.txt -H "Content-Type: application/json" -d '{
  "patientFullName":"Kasun Perera","patientAddress":"Colombo 05",
  "patientContactNumber":"0771234567","dentistId":1,"treatmentTypeId":1,
  "appointmentDate":"2026-09-01","appointmentTime":"09:00:00"
}' http://localhost:8082/api/appointments

# Calculate the bill
curl -b cookies.txt http://localhost:8082/api/bills/appointment/APT-2026-000001
```

### 6.3 Swagger UI

Open `http://localhost:8082/swagger-ui.html`, log in via the client first
(so the browser holds the auth cookie), then "Try it out" on any endpoint.

---

## 7. Validating the major features (walkthrough matching the assignment brief)

1. **User Authentication (Login)** — Open the client; try an invalid
   password (expect a clear "Invalid username or password" message and a
   `401`); then log in with `nadeesha`/`Nadeesha@123`.
2. **Register New Appointment** — Dashboard → *+ New Appointment*. Fill in a
   new patient or search for an existing one, choose a dentist and
   treatment type, pick a date/time, *Save*. Confirm the generated
   appointment number (`APT-…`).
3. **Display Appointment Details** — Appointments → *Find Appointment* →
   enter the appointment number → confirm all patient/dentist/treatment
   details display.
4. **Calculate and Print Bill** — From the appointment detail page, click
   *Generate / View Bill*; confirm the total (consultation fee + any
   weekend surcharge/loyalty discount + tax), then click *Print Receipt*
   (browser print dialog → *Save as PDF* works for evidence capture).
5. **Help Section** — Open *Help* in the navbar; confirm the step-by-step
   guide loads from `GET /api/help`.
6. **Exit System** — Click *Logout*; confirm you are redirected to the
   login page and that navigating back to a protected page (e.g. Dashboard)
   redirects you to Login again (the session cookie was cleared).
7. **Double-booking prevention** — Try registering a second appointment for
   the *same* dentist at the *same* date/time; expect an HTTP `409
   Conflict` with a clear message, both from the UI and confirmed at the
   database level by the `trg_prevent_double_booking` trigger.
8. **Reports** — Open *Reports*; confirm the daily revenue report (backed
   by the `sp_daily_revenue_report` stored procedure) and dentist
   utilisation report (backed by the `vw_dentist_utilization` view) both
   render.
9. **Admin-only screens** — Log in as `admin`; confirm *Staff Accounts* is
   visible and usable. Log in as `nadeesha`; confirm it is hidden, and that
   directly calling `POST /api/users` as staff returns `403 Forbidden`.
10. **Edit staff details (Admin)** — Log in as `admin` → *Staff Accounts* →
    click *Edit* on a staff row; confirm the modal opens pre-filled with
    that account's current name/email (username and role are shown but
    disabled - not editable here). Change the name/email and *Save
    Changes*; confirm the table updates immediately and `PATCH
    /api/users/{id}` returns the updated record. Confirm the same request
    as `nadeesha` (STAFF) returns `403 Forbidden`, and that a blank name or
    an invalid email is rejected with a `400` and an inline field error.

---

## 8. Running the automated test suite

```
cd SunriseDentalClinic\backend
.\mvnw.cmd test
```

This runs all JUnit 5 + Mockito unit tests and the full Spring Boot
integration test (real Spring context, real Spring Security filter chain,
in-memory H2 database) covering login, registration, double-booking
rejection, and billing. A summary is printed at the end (`Tests run: 25,
Failures: 0, Errors: 0`), and per-class reports are written to
`backend/target/surefire-reports/`. See `testing/TEST_PLAN.md` and
`testing/TEST_CASES.md` for the full rationale, test data, and traceability
matrix, and `testing/evidence/` for a captured passing run.

---

## 9. Troubleshooting common errors

### 9.1 `mysqld`/Apache won't start / phpMyAdmin shows connection errors / PHP warnings about missing extensions

**Cause:** if your XAMPP folder was moved or copied from its original
install location without running XAMPP's path-fixer, several config files
still contain the **old, hard-coded absolute paths** — this was encountered
and fixed on this machine during the companion VehicleReservationSystem
project's own setup (XAMPP moved to `F:\Program\xampp` while its configs
still referenced `F:\Program Files\xampp\...`), and the fix carries over
here since it modified the shared XAMPP install itself. If you see this on
a fresh machine, the same four files need the same treatment: `mysql\bin\my.ini`,
`apache\conf\httpd.conf`, `apache\conf\extra\httpd-xampp.conf`,
`php\php.ini` — replace every occurrence of the old install path with your
actual one and restart the affected service(s). Since this project's
backend talks to MySQL directly and the frontend is plain static files, you
only strictly need the `my.ini` fix to run this system.

### 9.2 Backend fails to start with a MySQL connection error

- Confirm MySQL is running (§2.1) and reachable on port `3306`.
- Confirm `sunrise_dental_clinic_db` exists (§2.2/§2.3).
- Check `spring.datasource.username`/`password` in `application.yml` match
  your MySQL root credentials (XAMPP's default is `root` with **no
  password**).

### 9.3 `401 Unauthorized` on every request from the browser client, even right after logging in

- The API and the client must be treated as different **origins** by the
  browser (e.g. `http://localhost:8082` vs `http://localhost`). Confirm the
  client's origin is present in `app.cors.allowed-origins` (§3.1) and
  restart the backend after editing it.
- Confirm cookies are not being blocked — the login flow relies on an
  HttpOnly cookie sent with `credentials: 'include'`; some browser privacy
  modes/extensions block third-party-looking cookies on `localhost`. Test
  in a normal (non-incognito) window first.

### 9.4 `403 Forbidden` instead of expected success

- You are logged in as `STAFF` but calling an `ADMIN`-only endpoint (e.g.
  `POST /api/users`, `DELETE /api/dentists/{id}`). Log in as `admin`
  instead.

### 9.5 `409 Conflict` when registering an appointment

- This is the double-booking guard working as intended — the chosen
  dentist already has an appointment at that exact date/time. Pick a
  different dentist or time, or check *Dentists* for one whose status is
  `AVAILABLE`.

### 9.6 Reports page shows an error

- The **Reports** screens depend on the stored procedure and view created
  by `database/schema.sql` (§2.2). If you only let Hibernate auto-create
  tables (`ddl-auto: update`) without ever running `schema.sql`, those
  objects will be missing. Re-run §2.2, Option B.

### 9.7 Port already in use (`8082` or `80`/`5501`)

- Change `server.port` in `application.yml` for the backend, or serve the
  frontend on a different port (§4) — just remember to add the new
  frontend origin to `app.cors.allowed-origins` (§3.1).

### 9.8 `mvnw.cmd` fails on the very first run

- This step needs internet access (it downloads Maven itself). If you are
  behind a proxy, configure it via the `MAVEN_OPTS` environment variable,
  or install Maven normally and run `mvn spring-boot:run` instead of
  `mvnw.cmd`.

---

## 10. Full stop / restart checklist

1. Backend: `Ctrl+C` in its terminal (or close the window).
2. Apache/MySQL: stop via the XAMPP Control Panel, or `mysql_stop.bat` /
   `apache_stop.bat` in the XAMPP root.
3. To reset all data to the original seed state at any time, simply re-run
   §2.2 — `database/schema.sql` drops and recreates everything.

---

## 11. Regenerating the PDF / Word report

`docs/ASSIGNMENT_REPORT.pdf` and `docs/ASSIGNMENT_REPORT.docx` are both
committed, ready-to-submit exports of `docs/ASSIGNMENT_REPORT.md`, already
formatted to the brief's spec (A4, margins 1.5in/1in, 1.5 line spacing,
Times New Roman, 14pt bold headings, 12pt body, page numbers bottom-right)
with every diagram and screenshot embedded. If you edit the report or
capture new screenshots, regenerate one or both:

```
cd docs
npm install          # first time only - installs marked, puppeteer-core, docx
node generate-pdf.js   # -> ASSIGNMENT_REPORT.pdf  (renders via headless Chrome)
node generate-docx.js  # -> ASSIGNMENT_REPORT.docx (native Word document via the `docx` library)
```

`generate-pdf.js` requires a local Chrome install (default path inside the
script; override with the `CHROME_PATH` environment variable if yours is
elsewhere). `generate-docx.js` has no such dependency — it parses the
Markdown into a token tree (via `marked`'s lexer) and builds native Word
paragraphs, tables and embedded images directly, so the `.docx` opens as a
real Word document in Microsoft Word / LibreOffice / Google Docs, not an
HTML file renamed. Both are one-off documentation tools, not part of the
running application — Node.js is not otherwise required anywhere in this
project.

To capture fresh screenshots first, log into the client at
`http://localhost/sunrise-dental-clinic-client/` (§5) and screenshot each
page listed in `testing/screenshots/`, or automate it with a
headless-browser script.
