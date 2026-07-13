# Spring Boot OIDC Authentication State Engine

Spring Boot 3.5 / Java 21 OIDC orchestration service for Auth0 logins, step-up MFA flows, audit trails, and browser-visible state-machine inspection.

## Project summary
The application externalizes the authentication journey into XML-defined workflows while keeping identity in Auth0 and handling authorization, session state, audit logging, and diagram rendering locally.

### Highlights
- XML-driven `login` and `step-up` flows
- Generic state-engine interpreter with guard-based transitions
- Step-up/MFA flow execution through the same engine path
- `POST /auth/step-up/verify` for MFA completion
- Federated Auth0 RP-initiated logout
- Explicit JWT / ID token clock-skew handling
- Per-session OAuth2 authorized-client persistence with refresh-token support
- Per-user session tracking with idempotent completion
- Authorization based on OIDC claims and allowed groups/roles
- Structured audit trail with correlation IDs
- JSON endpoints for flow, session, and audit inspection
- Browser flow rendering split into a dedicated view controller/service
- Actuator health/metrics and OpenAPI docs enabled
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
- `POST /auth/step-up/verify`
- `GET /auth/session`
- `GET /auth/session/{flowName}`
- `GET /auth/audit`

### Configuration
- `auth.jwt.allowed-clock-skew-seconds`
- `auth.logout.post-logout-redirect-uri`
- `auth.mfa.challenge-code`
- `auth.audit.max-records`
- `auth.allowed-groups`
- `spring.security.oauth2.client.*` / `application-oauth.yml`

### Local run
```bash
mvn spring-boot:run
```

### Tests
```bash
mvn test
```
- JaCoCo is configured to enforce 100% line and branch coverage.

### Postman
- `docs/postman/springboot-oidc-auth-state-engine.postman_collection.json`

### Roadmap / Known limitations
- Session cache, OAuth2 client persistence, and audit trail are in-memory only and reset on restart.
- This setup is single-instance only; multi-node production needs shared session/client storage.
- Graphviz `.dot` and generated SVG assets are checked in together and need manual regeneration when the diagrams change.
- Auth0/OIDC runtime settings still depend on local environment variables in `application-oauth.yml`.

### License
- MIT License (`LICENSE`)
