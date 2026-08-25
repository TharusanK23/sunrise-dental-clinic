# Git & GitHub Workflow (Task D)

## 0. Branching model

This repository intentionally uses **two long-lived branches** instead of a
single `main`, as directed for this project:

| Branch | Purpose |
|---|---|
| `dev` | The active integration branch. All day-to-day work (features, fixes, docs) is committed here first. This is the repository's default branch. |
| `release` | The stable, submission-ready branch. Only fast-forwarded from `dev` at deliberate checkpoints once the code on `dev` builds cleanly and the full test suite is green — never committed to directly. |

**Repository:** https://github.com/KirishaTharusan/sunrise-dental-clinic (public).

## 1. What has been set up locally

This project has been initialised as a local Git repository (`git init -b
dev`) with a **meaningful, milestone-based commit history** (one commit per
major deliverable — project scaffold, backend domain/patterns, security,
automated tests, frontend, diagrams, documentation — rather than one single
"initial commit"), a `.gitignore` tuned for a Java + static-frontend
project, and a GitHub Actions workflow (`.github/workflows/ci.yml`) that
builds and runs the full JUnit suite on every push to `dev` or `release`.
Run `git log --oneline --graph --all` from `SunriseDentalClinic/` to see it.

## 2. How `dev` and `release` are used together

1. All work happens on `dev` (or a short-lived `feature/<name>` branch
   merged back into `dev`), with small, dated, descriptive commits — e.g.
   "Add Strategy pattern for consultation billing", "Add frontend
   appointment registration form", "Add integration tests".
2. `Backend CI` runs automatically on every push to `dev` (and to any
   `feature/**` branch) — see `.github/workflows/ci.yml`. This is the
   continuous-integration feedback loop: it must be green before that work
   is considered done.
3. Once a coherent set of changes on `dev` builds and tests cleanly, it is
   **fast-forwarded into `release`**:
   ```bash
   git checkout release
   git merge --ff-only dev
   git push origin release
   ```
   `release` therefore always points at a commit that is known-good and
   ready to be marked/submitted — it is never force-pushed or rebased.
4. A tag is cut on `release` once the coursework is feature-complete:
   ```bash
   git tag -a v1.0 -m "Submission version for CIS6003 WRIT1"
   git push origin v1.0
   ```

## 3. Recommended ongoing workflow for the rest of the assignment period

The brief specifically asks for *"several versions ... updated each day"*
with visible version control technique. Please follow this pattern for any
further changes (report edits, extra features, bug fixes) between now and
submission:

1. **Work on `dev`**, not directly on `release`:
   ```bash
   git checkout dev
   # make changes
   git add <files>
   git commit -m "Add <short description of the change>"
   git push origin dev
   ```
2. For a larger or riskier change, branch off `dev` instead
   (`git checkout -b feature/<short-description>`), open a **Pull Request**
   on GitHub back into `dev`, let `Backend CI` go green, then merge. A
   screenshot of a merged PR with a green CI check is exactly the
   "workflow (CI/CD) demonstrated" evidence the marking criteria ask for —
   include it in the assignment report's appendix.
3. **Commit in small, dated increments** rather than one giant commit at
   the end. Each real commit carries its own author date automatically —
   there is no need to fake this, just commit as you actually work.
4. **Promote to `release`** (§2 step 3) once `dev` is stable again.

## 4. Version-control techniques demonstrated in this project

| Technique | Where |
|---|---|
| `.gitignore` scoped to a mixed Java/static-frontend project | `.gitignore` |
| Milestone-based, descriptive commit messages | `git log` |
| A dedicated integration branch (`dev`) separate from the stable branch (`release`) | `git branch -a` |
| Branch-per-feature + Pull Request merge workflow | §3 above (to be performed on GitHub after push) |
| Continuous Integration (build + automated test run on every push) | `.github/workflows/ci.yml` |
| Semantic version tagging | §2 step 4 |
| A `README.md` and `docs/` as the entry point for anyone cloning the repo | `README.md`, `docs/` |

## 5. Repository visibility

Make sure the repository is created as **Public** (Settings → General →
Danger Zone → Change visibility, if it was accidentally created Private) so
it is accessible for marking, and confirm its default branch is set to
`dev` (Settings → General → Default branch) since that is where ongoing
work happens.
