# UML, ER & Flowchart Diagrams

This folder contains the full set of design diagrams required by **Task A** of the
CIS6003 assignment brief (Sunrise Dental Clinic scenario), plus the
Entity-Relationship diagram for the database (supporting Task B's "proper
database" requirement) and a program flowchart of the overall menu-driven
application logic.

| File | Diagram | Notes |
|---|---|---|
| `use-case-diagram.mmd` / `.png` | Use Case Diagram | Actors, use cases, `<<include>>`/`<<extend>>` stereotypes |
| `class-diagram.mmd` / `.png` | Class Diagram | Domain model + service layer + all 5 design patterns |
| `sequence-01-login.mmd` / `.png` | Sequence Diagram 1 | User Authentication (Login) |
| `sequence-02-register-appointment.mmd` / `.png` | Sequence Diagram 2 | Register New Appointment |
| `sequence-03-generate-bill.mmd` / `.png` | Sequence Diagram 3 | Calculate and Print Bill |
| `er-diagram.mmd` / `.png` | Entity Relationship Diagram | Database schema (7 tables) |
| `flowchart.mmd` / `.png` | Program Flowchart | Menu-driven control flow: login → main menu → every function (with its validation/error loop-backs) → exit |

All diagrams are authored as [Mermaid](https://mermaid.js.org) source (`.mmd`) and
pre-rendered to `.png` so they can be viewed without any tooling. Mermaid also
renders `.mmd`/fenced-mermaid content natively on GitHub and in Claude
Artifacts, so these files are directly viewable there too, not just as static
images. To regenerate the PNGs after an edit:

```bash
cd diagrams
npx @mermaid-js/mermaid-cli -i class-diagram.mmd -o class-diagram.png -b white -s 2
# repeat per file, or: for f in *.mmd; do npx @mermaid-js/mermaid-cli -i "$f" -o "${f%.mmd}.png" -b white -s 2; done
```

(Requires Node.js; `mmdc` launches a headless Chromium via Puppeteer the first
time it runs, which it downloads automatically. If a previous partial/broken
npx install of `@mermaid-js/mermaid-cli` is cached, clear it from
`%LOCALAPPDATA%\npm-cache\_npx\` and re-run.)

**Note on the Class, Sequence and ER diagrams vs. the Use Case diagram:**
Mermaid has first-class native syntax for `classDiagram`, `sequenceDiagram`
and `erDiagram`, so those five diagrams use proper UML/ER constructs
(visibility modifiers, `<<interface>>`/`<<abstract>>` stereotypes,
inheritance/realisation/aggregation/composition arrows, crow's-foot
cardinality). Mermaid has **no** native UML use-case diagram type, so
`use-case-diagram.mmd` is modelled with its general-purpose `flowchart`
syntax instead — actors as labelled nodes, use cases as stadium-shaped nodes
inside a system-boundary subgraph, and `<<include>>`/`<<extend>>` as
labelled dashed edges — the closest faithful equivalent the tool supports,
while still showing every actor, use case, and stereotype relationship
required by Task A.

**`flowchart.mmd`** uses that same `flowchart` syntax for its intended
purpose — a plain program flowchart, not a UML diagram — tracing the
menu-driven control flow of the whole application (login, main-menu
dispatch, each function's internal branching, and exit). It complements
the Use Case diagram: the Use Case diagram documents *capabilities* (what
actors can do), while the flowchart documents *control flow* (the order
operations actually happen in, including validation/error loop-backs) — see
`docs/ASSIGNMENT_REPORT.md` for the full explanation of both diagrams together.

## Scenario followed

Unlike the companion `VehicleReservationSystem` project (which adapted a
mismatched brief into a vehicle-rental domain), this project implements the
**Sunrise Dental Clinic** scenario exactly as described in
`Sunris-Dental-Clinic-System-Assignment-brief.pdf`: patients register once and
are reused across visits; a staff member books an **appointment** with a
named **dentist** for a **treatment type** (each with its own consultation
fee) at a single **appointment date & time**; a **bill** is calculated from
the treatment type's consultation fee plus any weekend surcharge or loyalty
discount and 8% tax; only authorised staff can use the system.

## Design decisions & assumptions (Task A)

1. **Two roles, not one.** The brief says "only authorised staff can use the
   system" but does not specify roles. `ADMIN` and `STAFF` were introduced
   because the Excellent-band marking criteria explicitly reward role-based
   access and a more sophisticated data/business model. `ADMIN` manages
   dentists, treatment types and staff accounts; `STAFF` performs the
   day-to-day booking/billing workflow described in the brief.
2. **A single appointment date/time, not a range.** Unlike a multi-day
   rental, a clinic visit is a single point in time — this matches the
   brief's wording ("appointment date and time") exactly, without adding an
   unrequested return/end date.
3. **Patients are first-class, reusable records**, not re-entered on every
   booking — mirroring how the brief's registration data (name/address/
   contact) is described once and then referenced by "appointment number"
   thereafter.
4. **A bill is generated once per appointment and persisted**, rather than
   recalculated on every view. This avoids a bill silently changing if a
   treatment type's consultation fee is edited later, which is both a more
   realistic business rule and a cleaner demonstration of the Factory
   pattern (Task B).
5. **Double-booking prevention** is treated as a first-class business rule
   (not explicitly stated in the brief, but essential to a real clinic
   scheduler) because a dentist cannot be in two appointments at once. It is
   enforced twice, deliberately: once in `AppointmentServiceImpl` (a clear
   error message to the user) and once again as a MySQL trigger
   `trg_prevent_double_booking` (a database-level invariant that holds even
   if a future client bypasses the REST API) — see `database/schema.sql`.
6. **A loyalty discount, not a long-term-stay discount.** A vehicle rental
   naturally rewards a longer single booking; a dental clinic instead
   naturally rewards a *returning* patient, so the discount strategy is keyed
   off the patient's count of prior **completed** visits (≥ 5) rather than
   the duration of the current booking.
7. **"Exit System"** is interpreted, for a web application, as a secure
   Logout that clears the authentication cookie server-side — the closest
   safe-shutdown equivalent for a browser-based client. See
   `docs/ASSIGNMENT_REPORT.md` for the full justification.

## How the diagrams support the design (traceability)

- The **Use Case Diagram** enumerates every functionality from the brief
  (Login, Register New Appointment, Search/Display, Calculate & Print Bill,
  Help, and Logout/Exit) plus the additional Admin-only use cases, and shows
  the two `<<include>>` relationships that are *always* executed (checking
  dentist availability, calculating a base cost) versus the three
  `<<extend>>` relationships that are *conditional* (new-patient
  registration, weekend surcharge, loyalty discount).
- The **Class Diagram** shows exactly which classes implement each of the
  five design patterns used in Task B (Builder, Factory Method, Strategy,
  Observer, Singleton), with correct public/private visibility, and the
  aggregation/composition/multiplicity of the domain model (e.g. an
  `Appointment` *contains* at most one `Bill` — composition — but only
  *references* a `Patient`/`Dentist` — aggregation).
- The three **Sequence Diagrams** trace the three most important use cases
  end-to-end, from the browser through the controller/service/repository
  layers and down to MySQL, including the specific point at which each
  design pattern is invoked.
- The **ER Diagram** matches `database/schema.sql` exactly (table/column
  names, primary/foreign keys, and the derived `notification_logs`
  correlation).
- The **Flowchart** ties all of the above together operationally: it is the
  only diagram that shows the *order* things happen in and what happens on
  every failure path (invalid login, dentist unavailable, validation
  errors, a double-booking conflict), directly matching the brief's
  "menu driven application" description and the client's actual page flow.
