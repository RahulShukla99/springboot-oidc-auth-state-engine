# Spring Boot OIDC Authentication State Engine

High-level goal: externalize the post-login authentication journey into a deterministic XML workflow so Auth0 handles identity and the application handles authorization, session state, and audit trails.

## Project summary
Built a Spring Boot 3.5 / Java 21 authentication orchestration service for Auth0/OIDC logins.

### Highlights
- XML-driven login and step-up/MFA flows
- Auth session tracking with per-user idempotency
- Authorization based on OIDC claims and groups/roles
- Bounded audit trail with correlation IDs
- JSON endpoints for flow, session, and audit inspection
- Docker, React UI, and AWS-ready deployment model

### Core flows
- `login`
- `step-up`

### Endpoints
- `GET /`
- `GET /auth/flow/{flowName}`
- `GET /auth/success/{flowName}`
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
