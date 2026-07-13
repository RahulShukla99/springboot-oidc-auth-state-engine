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
