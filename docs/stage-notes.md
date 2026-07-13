# Stage Notes

## 2026-07-05 Graphviz-backed browser flow views

- Implemented behavior: `/auth/flow/view` and `/auth/flow/step-up/view` now render server-side SVG diagrams instead of relying on client-side Mermaid.
- Assumption: the browser must show the attachment-style diagram reliably even when third-party scripts are blocked or unavailable.
- Business rules covered: both flows preserve the configured initial state, final states, and transition labels while presenting a readable top-to-bottom state engine diagram.
- Edge cases: inline SVG content strips the XML declaration before embedding so browser HTML parsing does not corrupt the diagram.
- Concurrency decision: none; rendering uses static classpath resources only.
- Known limitation: Graphviz source files are checked in, but local `dot` is not installed here, so regeneration is manual for now.
- Next improvement: add a build step or developer script to regenerate the SVG artifacts from the `.dot` files when Graphviz is available.

## 2026-07-06 Explicit SVG presentation attributes

- Implemented behavior: the two shipped flow diagrams now use explicit SVG fill, stroke, font, and marker attributes so the browser renders the intended design without depending on embedded SVG CSS.
- Assumption: inline SVG styling was being interpreted inconsistently in the browser, while presentation attributes remain stable for the state-node colors and edge-label layout.
- Business rules covered: the login and step-up MFA flows now keep the correct START, failure, and success styling and preserve readable transition labels beside the intended arrows.
- Edge cases: both diagrams keep the same accessibility metadata and the same geometry used by the label-spacing tests.
- Concurrency decision: none; only static resource markup changed.
- Known limitation: the Graphviz `.dot` files and the checked-in SVGs can still drift if one is updated without regenerating the other.
- Next improvement: add a repeatable regeneration step so the `.dot` definitions stay aligned with the browser-facing SVG artifacts.

## 2026-07-13 Generic auth-state interpreter

- Implemented behavior: the engine now walks transitions generically from the current state and resolves the next event through flow-specific guard rules instead of a hardcoded event chain.
- Assumption: both XML flows should execute through the same interpreter path, with business differences expressed as guards rather than separate orchestration code.
- Business rules covered: the login flow still reaches `AUTH_SUCCESS` / `AUTH_FAILED`, while the step-up flow now reaches `STEP_UP_SUCCESS` / `AUTH_FAILED` end-to-end, including `MFA_PASSED` and `MFA_FAILED`.
- Edge cases: failure reasons are preserved when already present, and the engine fails fast when no transition guard matches a state.
- Concurrency decision: none yet; the interpreter is stateless per execution and relies on the existing in-memory audit service.
- Known limitation: session state, audit trail, and all flow execution data are still JVM-local.
- Next improvement: add the MFA challenge/verify endpoint and then expand security/logout/token handling.

## 2026-07-13 Step-up execution and security hardening

- Implemented behavior: `POST /auth/step-up/verify` now drives the existing step-up XML flow end-to-end using the same generic engine path as login.
- Assumption: the MFA challenge can be represented by a minimal in-memory service that issues and verifies a simple code for the authenticated user.
- Business rules covered: the step-up flow completes to `STEP_UP_SUCCESS` when MFA passes and to `AUTH_FAILED` when it fails, while keeping the session idempotent per user and flow.
- Edge cases: blank MFA codes are rejected, and the handler preserves the existing login/session behavior for the authenticated user.
- Concurrency decision: reused the existing per-user/per-flow session cache; no new shared mutable workflow state was added.
- Implemented behavior: logout now redirects to Auth0's federated end-session endpoint, and ID token validation now uses an explicit 60s skew budget.
- Known limitation: logout, MFA challenge codes, and token validation are still in-memory/demo-oriented and depend on local configuration.
- Next improvement: refresh-token persistence and controller/view separation.

## 2026-07-13 Refresh-token persistence, actuator/OpenAPI, and view split

- Implemented behavior: OAuth2 authorized clients now persist per HTTP session, enabling refresh-token reuse when an access token expires.
- Assumption: per-session in-memory client storage is sufficient for the local demo, while production would externalize the client/session store.
- Business rules covered: expired access tokens are refreshed through the authorized-client manager, actuator health/metrics are reachable under security rules, and OpenAPI endpoints remain available for authenticated users.
- Edge cases: the custom authorized-client repository only serves session-scoped OAuth2 clients and rejects blank principals; browser flow HTML rendering is now isolated from JSON endpoints.
- Concurrency decision: used session-scoped storage plus atomic map operations to avoid check-then-act client overwrite races.
- Known limitation: the repository is still JVM-local and does not coordinate across multiple application instances.
- Next improvement: back the client/session cache with a shared store and add production hardening for multi-node deployments.

## 2026-07-13 Configurable defaults and coverage enforcement

- Implemented behavior: audit retention now reads from `auth.audit.max-records` instead of a Java-side fallback, and the build now enforces JaCoCo line/branch coverage.
- Assumption: application defaults belong in `application.yml` / `application-example.yml`, while tests can still instantiate the service directly with explicit values.
- Business rules covered: the in-memory audit trail keeps only the configured number of recent records and rejects invalid retention values.
- Edge cases: the Spring bean uses `AuthAuditProperties`, which keeps the configuration path explicit without adding a second hidden default.
- Concurrency decision: unchanged; audit writes remain synchronized around the bounded deque.
- Known limitation: browser diagram sizing remains static presentation data rather than property-driven configuration.
- Next improvement: if needed, extract the remaining presentation constants into a dedicated view-properties object.
