# Spring Boot OIDC Authentication State Engine

Spring Boot 3.5 / Java 21 OIDC orchestration service for Auth0 logins, step-up MFA flows, audit trails, and browser-visible state-machine inspection.

## Project summary
The application externalizes the authentication journey into XML-defined workflows while keeping identity in Auth0 and handling authorization, session state, audit logging, and diagram rendering locally.

### Highlights
- XML-driven `login` and `step-up` flows
- Generic state-engine interpreter with guard-based transitions
- Step-up/MFA flow execution through the same engine path
- Per-user session tracking with idempotent completion
- Authorization based on OIDC claims and allowed groups/roles
- Structured audit trail with correlation IDs
- JSON endpoints for flow, session, and audit inspection
- Browser flow diagrams rendered from checked-in SVG resources

### Core flows
- `login`
- `step-up`

### Browser flow views
- `GET /auth/flow/view`
- `GET /auth/flow/step-up/view`

### API endpoints
- `GET /`
- `GET /auth/flow`
- `GET /auth/flow/{flowName}`
- `GET /auth/success`
- `GET /auth/success/{flowName}`
- `GET /auth/session`
- `GET /auth/session/{flowName}`
- `GET /auth/audit`

### Local run
```bash
mvn spring-boot:run
```

### Tests
```bash
mvn test
```

### Postman
- `docs/postman/springboot-oidc-auth-state-engine.postman_collection.json`

### Roadmap / Known limitations
- Session cache and audit trail are in-memory only and reset on restart.
- Federated logout, clock-skew handling, refresh-token persistence, actuator/OpenAPI exposure, and controller split are still pending.
- Graphviz `.dot` and generated SVG assets are checked in together and need manual regeneration when the diagrams change.

### License
- MIT License (`LICENSE`)
