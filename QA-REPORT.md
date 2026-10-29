# CareerTrack QA report

Tested locally on 29 October 2026 using Windows, Node.js 24.15, Java 22
(compiling for Java 17), MongoDB 8.2.6, and headless Google Chrome.

## Verification results

| Check | Result |
| --- | --- |
| Backend Maven tests and executable JAR build | Passed: 26 tests, zero failures/errors |
| Frontend production build | Passed with Vite 7.3.7 |
| Frontend dependency audit | Zero known vulnerabilities after updates |
| Root test-tool dependency audit | Zero known vulnerabilities |
| API smoke checks | Passed: registration, login, applications, interviews, statistics, filters, and auth errors |
| Live API regression checks | Passed: validation, account overwrite prevention, ownership checks, interview cleanup, and configured CORS |
| Chrome workflow checks | Passed: registration, wrong password, login, application create/edit/filter/delete, interviews, cancel editing, persistence, and expired sessions |
| Browser race checks | Passed: duplicate submissions and delayed requests across logout/login |
| Responsive checks | Passed: no horizontal overflow at 320, 375, 768, and 1440 pixels |
| Portfolio checks | Passed: responsive layout and mobile menu opening/closing |
| JavaScript syntax and Git whitespace checks | Passed |

The live suite used its own disposable MongoDB process and database. It stopped
its backend/frontend/database processes afterward. No existing database or hosted
application was modified by these tests.

## Bugs fixed

- Registration could replace an existing account's password when the account had
  no applications. Existing accounts now remain unchanged. External-profile retries
  require the original password and store name, and completed registrations are rejected.
- Registration and application startup silently deleted duplicate user records.
  Removed automatic deletion to prevent loss of accounts and orphaned records.
- Interviews accepted missing fields, nonexistent application IDs, and another
  user's application ID. Creation and updates now validate the request and ownership.
- Application creation and updates accepted blank companies/job titles. Required
  fields now receive server-side validation. Malformed dates and invalid status
  parameters return a useful HTTP 400 response.
- Deleting an application left its interviews behind. Associated interviews for
  that owner are now removed as part of the delete flow.
- Any authenticated user could retrieve another user's profile by ID. The profile
  endpoint now returns only the caller's profile.
- CORS configuration ignored the configured origin. The backend now respects the
  configured comma-separated origin list. JWT signing secrets can be supplied by environment.
- Repeated form submissions could create duplicate records. Pending requests now
  disable mutation controls and guard duplicate handlers.
- Responses from an old session could affect a new session. Session/request
  versions now prevent stale data and stale errors from updating the current UI.
- Deleting an application while editing left the form pointing at the deleted
  record. Deletion now resets that form and its interview selection. Added cancel editing.
- Application dates used UTC and a value captured when the module loaded. New
  forms now calculate today's local date when initialized/reset.
- Interview submission unconditionally appended seconds to the input. It now
  sends the browser's valid local date/time without altering its format.
- Invalid stored session objects were treated as logged-in users. Invalid session
  data is now cleared, and logout clears credentials and registration feedback.
- Local frontend development defaulted to the hosted API. It now defaults to the
  local API through `.env.development`; shell overrides and development-local
  environment settings still take precedence. Production settings are preserved.
- Added accessible names for tracker controls, long-text wrapping, and correct
  expanded state for the portfolio menu. Removed a redundant nested contact wrapper.
- Updated vulnerable frontend dependencies and upgraded Vite and its React plugin.
- Smoke tests created external jeweller profiles automatically and hardcoded a
  hosted CORS origin. External profile creation is now opt-in and the origin configurable.

## Repeat the checks

Follow the automated-check commands in `README.md`. The root `test:e2e` script
requires a packaged backend JAR and starts all its own test services. The first
run downloads MongoDB; Playwright requires bundled Chromium or an installed browser
selected through `QA_BROWSER_CHANNEL`.

Maven was not on this machine's PATH, so this run used a temporary Maven 3.9.9
installation. Add Maven to PATH to use the documented `mvn` commands directly.

## Scope and remaining limits

- The optional jeweller service was covered by mocked service/controller tests
  (success, retry, conflict, missing configuration). Its live external endpoint and
  deployment credentials were not exercised.
- These are functional/regression tests, not a load test, exhaustive security
  assessment, or a guarantee that every possible defect is eliminated.
- Hosted Render/Vercel deployments were not redeployed or tested. Deployment
  environment settings and behavior still need verification after publishing.
- Existing duplicate users or orphaned interviews were not migrated or deleted.
  The fixes prevent the identified destructive startup behavior and normal-flow
  orphan creation; they do not repair historical data.
- Multi-instance concurrent registration and database transaction failure scenarios
  were not tested. External profile creation retains its documented idempotency limitations.
- The pre-existing Eclipse `.project` and JDT preferences edits were left untouched.
