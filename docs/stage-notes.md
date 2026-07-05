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
