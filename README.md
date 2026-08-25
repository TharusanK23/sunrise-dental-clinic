# Sunrise Dental Clinic — Online Appointment & Patient Management System

A full-stack Java coursework project for **CIS6003 Advanced Programming**
(Cardiff Metropolitan University / ICBT Campus), implementing a distributed,
three-tier appointment and billing system with a Spring Boot REST API, a
MySQL database (via XAMPP), and a static HTML/CSS/JS client.

**Student:** Kirisha – JF/BSCSD/19/33 – BSc SE (Top-Up)
**Repository:** https://github.com/KirishaTharusan/sunrise-dental-clinic

> **Read [`docs/SETUP.md`](docs/SETUP.md) first** — it has the complete
> install/run/test/troubleshoot walkthrough for localhost.

## Project layout

```
SunriseDentalClinic/
├── backend/             Spring Boot 3 (Java 17) REST API — Maven project (use ./mvnw)
├── frontend/            Static HTML/CSS/JS client (Bootstrap 5, vendored locally)
├── database/            MySQL schema.sql (tables, triggers, procedure, views, seed data)
├── diagrams/            Use Case / Class / Sequence x3 / ER / Flowchart diagrams (Mermaid + PNG)
├── testing/             Test plan, test case matrix, Postman collection, test evidence
├── docs/                SETUP.md, the assignment report, and the Git workflow write-up
└── .github/workflows/   CI pipeline (build + test on every push to dev/release)
```

## Quick start

```
1. Start MySQL (XAMPP) and import database/schema.sql
2. cd backend && ./mvnw.cmd spring-boot:run        (API on http://localhost:8082)
3. Serve frontend/ via XAMPP Apache (or any static server) on http://localhost/sunrise-dental-clinic-client/
4. Log in with admin / admin123 or kirisha / Kirisha@123
```

Full details, including troubleshooting XAMPP-specific issues, are in
[`docs/SETUP.md`](docs/SETUP.md).

## What's implemented

Every functionality from the assignment brief (login, register a new
appointment, search/display appointment details, calculate & print a bill, a
help section, and a safe exit/logout) plus the Task B requirements: a
distributed REST API, five GoF design patterns, a three-tier architecture,
and a MySQL database with triggers/a stored procedure/views for advanced
reporting.

See [`docs/ASSIGNMENT_REPORT.md`](docs/ASSIGNMENT_REPORT.md) for the full
write-up (design rationale, pattern justification, testing strategy, and
evaluation against the marking criteria), and
[`diagrams/README.md`](diagrams/README.md) for the documented assumptions
behind the design.

## Branching

This repository uses a `dev` / `release` branching model — see
[`docs/GIT_WORKFLOW.md`](docs/GIT_WORKFLOW.md) for the full rationale and
ongoing workflow.
