# Spring Boot OIDC Authentication State Engine

Enterprise-style authentication orchestration service built with Spring Boot 3.5.x, Java 21, Spring Security OAuth2 Client, Auth0/OIDC, React, Lombok, Maven, and an XML-defined authentication state engine.

This project is not a basic Auth0 login sample. Auth0 performs identity authentication, while the Spring Boot application owns the post-login journey: callback processing, token/profile checks, authorization decisions, state transitions, final outcome selection, and transition-level audit logging.

## Why this project exists

Modern IAM platforms authenticate users, but enterprise applications often need additional application-owned orchestration after login: profile enrichment, authorization rules, failure routing, audit records, correlation IDs, and configurable journey logic. This project demonstrates that separation of responsibilities.

## Architecture

```text
Browser
  |
  | GET /oauth2/authorization/auth0
  v
Spring Security OAuth2 Client
  |
  | Redirect to Auth0 Universal Login
  v
Auth0 / OIDC Provider
  |
  | Callback: /login/oauth2/code/auth0
  v
Spring Boot Application
  |
  |-- OIDC principal extraction
  |-- AuthSessionContext creation
  |-- XML AuthStateEngine execution
  |-- AuthorizationService group/role checks
  |-- In-memory transition audit log
  v
JSON authentication journey result
```

## Package structure

```text
com.rahulshukla.authengine
  audit       transition audit records and bounded in-memory audit store
  frontend    React/Vite UI for login, flow metadata, and audit inspection
  config      Spring Security and auth engine bean configuration
  controller  REST endpoints for login result, session, flow, and audit views
  engine      XML flow loader and deterministic state engine
  exception   domain exceptions and JSON error handling
  model       immutable flow model and authentication session context
  service     authorization and session services
```

## Authentication journey

The flow is configured in `src/main/resources/auth-flow.xml`:

```text
START --LOGIN_REQUESTED--> REDIRECT_TO_IDP
REDIRECT_TO_IDP --OIDC_CALLBACK_RECEIVED--> VALIDATE_TOKEN
VALIDATE_TOKEN --TOKEN_VALID--> LOAD_USER_PROFILE
VALIDATE_TOKEN --TOKEN_INVALID--> AUTH_FAILED
LOAD_USER_PROFILE --PROFILE_LOADED--> AUTHORIZE_USER
AUTHORIZE_USER --USER_AUTHORIZED--> AUTH_SUCCESS
AUTHORIZE_USER --USER_NOT_AUTHORIZED--> AUTH_FAILED
```

The XML loader validates the flow before the app starts:

- exactly one initial state
- unique non-blank state IDs
- non-blank transition events and targets
- every transition target exists
- no outgoing transitions from final states
- duplicate transition events from the same state are rejected
- malformed XML returns a clear startup exception

## Security configuration

Spring Security is configured for OAuth2/OIDC login using the `auth0` registration.

- Login URL: `/oauth2/authorization/auth0`
- Callback URL: `/login/oauth2/code/auth0`
- Public endpoints: `/`, `/auth/flow`, `/error`
- Authenticated endpoints: `/auth/success`, `/auth/session`, `/auth/audit`
- Session fixation protection enabled
- CSRF token repository configured
- Security headers include CSP, frame denial, and no-referrer policy
- No secrets are committed; all client secrets come from environment variables

## Auth0 setup

1. Create an Auth0 **Regular Web Application**.
2. Configure Allowed Callback URLs:

   ```text
   http://localhost:8080/login/oauth2/code/auth0
   ```

3. Configure Allowed Logout URLs if needed:

   ```text
   http://localhost:8080/
   ```

4. Copy your client ID, client secret, and issuer URI.
5. Optional: add `groups` or `roles` claims to ID tokens using Auth0 Actions.

Auth0 does not include group/role claims in ID tokens by default. If no group claims are present, this demo allows a user when the email is present and verified. If groups are present, at least one must match `auth.allowed-groups`.

## Local secrets configuration

No Auth0 tenant URL, client ID, client secret, or API key should be committed to git.

The default profile starts without OAuth client registration so the project can run safely from a clean checkout. Use either environment variables with the `oauth` profile or a local Spring profile file for real Auth0 login.

### Option 1: environment variables with the `oauth` profile

Bash:

```bash
export AUTH0_CLIENT_ID=your-client-id
export AUTH0_CLIENT_SECRET=your-client-secret
export AUTH0_ISSUER_URI=https://your-auth0-domain.us.auth0.com/
export AUTH_ALLOWED_GROUPS=APP_USER,APP_ADMIN
export AUTH_AUDIT_MAX_RECORDS=100
```

PowerShell:

```powershell
$env:AUTH0_CLIENT_ID="your-client-id"
$env:AUTH0_CLIENT_SECRET="your-client-secret"
$env:AUTH0_ISSUER_URI="https://your-auth0-domain.us.auth0.com/"
$env:AUTH_ALLOWED_GROUPS="APP_USER,APP_ADMIN"
$env:AUTH_AUDIT_MAX_RECORDS="100"
```

Run with the OAuth profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=oauth
```

### Option 2: local Spring profile file

Copy the example file:

```bash
cp src/main/resources/application-example.yml src/main/resources/application-local.yml
```

PowerShell:

```powershell
Copy-Item src/main/resources/application-example.yml src/main/resources/application-local.yml
```

Fill in local values using placeholders as a guide:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          auth0:
            client-id: your-client-id
            client-secret: your-client-secret
        provider:
          auth0:
            issuer-uri: https://your-auth0-domain.us.auth0.com/
```

Run with the local profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

`src/main/resources/application-local.yml`, root-level `application-local.yml`, and `.env` are ignored by git.

## Run locally

Without OAuth configured, the app starts in documentation/demo mode:

```bash
mvn spring-boot:run
```

With Auth0 configured, use either the `oauth` or `local` profile shown above.

Open:

```text
http://localhost:8080/
```

Start OIDC login:

```text
http://localhost:8080/oauth2/authorization/auth0
```

## Endpoints

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/` | Public | Welcome message and login URL |
| GET | `/auth/flow` | Public | Loaded XML flow rendered as JSON |
| GET | `/auth/success` | Authenticated | Executes the post-login state engine |
| GET | `/auth/session` | Authenticated | Current OIDC user and final auth state |
| GET | `/auth/audit` | Authenticated | Recent bounded in-memory transition audit records |

## Sample state engine response

```json
{
  "correlationId": "7f8f3f1c-8c5b-45db-95e1-2d890e6d50c6",
  "username": "user@example.com",
  "fullName": "Example User",
  "emailVerified": true,
  "groupsOrRoles": ["APP_USER"],
  "currentState": "AUTH_SUCCESS",
  "finalState": "AUTH_SUCCESS",
  "failureReason": null,
  "transitionHistory": [
    {
      "fromState": "START",
      "event": "LOGIN_REQUESTED",
      "toState": "REDIRECT_TO_IDP",
      "timestamp": "2026-07-04T21:00:00Z"
    }
  ]
}
```

## Sample audit record

```json
{
  "correlationId": "7f8f3f1c-8c5b-45db-95e1-2d890e6d50c6",
  "username": "user@example.com",
  "fromState": "AUTHORIZE_USER",
  "event": "USER_AUTHORIZED",
  "toState": "AUTH_SUCCESS",
  "timestamp": "2026-07-04T21:00:01Z",
  "outcome": "SUCCESS"
}
```

## Error handling

Application errors are returned as clean JSON:

```json
{
  "timestamp": "2026-07-04T21:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Transition target MISSING does not exist",
  "path": "/auth/flow"
}
```

Unexpected exceptions are logged server-side and return a generic message to avoid leaking internals.

## Tests

Run:

```bash
mvn test
```

The test suite covers:

- valid XML flow loading
- duplicate state rejection
- blank state ID rejection
- missing transition target rejection
- valid and invalid state transitions
- failed authorization audit outcome
- bounded audit log retention
- verified OIDC user authorization
- missing email rejection
- unverified email rejection
- missing groups fallback behavior
- disallowed group rejection

## IAM / OAuth2 / OIDC concepts demonstrated

- Externalized identity authentication with Auth0/OIDC
- Spring Security OAuth2 Client login flow
- Environment-based secret management
- Application-owned post-login orchestration
- Configurable XML state machine for auth journeys
- OIDC profile and email verification checks
- Role/group-based authorization with documented fallback
- Correlation IDs and transition audit records
- Security headers and session fixation protection
- Lombok usage for constructor, accessor, and logger boilerplate reduction
- Clean operational error responses

## React UI

The project uses a React/Vite frontend instead of Thymeleaf.

```bash
cd frontend
npm install
npm run dev
```

For local API proxying during frontend development, run the Spring Boot API on port `8080` and access the login URL directly from the React UI.

## Idempotency, concurrency, locking, and retries

Current implementation choices:

- **Idempotency:** post-login orchestration is idempotent per username/email within one application instance. `AuthSessionService.findOrCreateCompletedSession` uses `ConcurrentHashMap.computeIfAbsent` so concurrent callbacks for the same user create and execute only one completed session result.
- **Concurrency lock:** no global lock is used. The lock scope is one username key inside `ConcurrentHashMap`, reducing contention and avoiding unrelated users blocking each other.
- **Deadlock reduction:** there are no nested locks, no cross-key locking, and no blocking network calls inside custom locks. The state engine operates on a per-session context.
- **Transaction boundaries:** this sample is intentionally in-memory, so there is no database transaction. In production, store session/audit records in a database transaction around one auth journey result, with a unique constraint on an idempotency key such as correlation ID or provider subject.
- **Retry behavior:** the app does not retry OAuth2/OIDC login callbacks internally. Browser or IdP retries are handled idempotently by returning the existing session result for the same username. Production retries for downstream dependencies should be bounded, timeout-driven, and only used for safe/idempotent operations.
- **Audit storage:** in-memory audit records are bounded by `AUTH_AUDIT_MAX_RECORDS`. Production should use an append-only durable audit sink.

## Production notes

For a production deployment, replace in-memory session/audit storage with persistent storage, add structured log forwarding, use a centralized audit sink, add rate limiting, and enforce HTTPS at the edge. In a multi-instance deployment, do not rely on in-memory state for compliance-grade audit history. Use database uniqueness plus optimistic locking for cross-instance idempotency.

## Resume bullet

Designed and developed a Spring Boot authentication orchestration service using OAuth2/OIDC with Auth0/Okta as the external Identity Provider. Implemented an XML-driven state engine to manage login, callback handling, token validation, profile loading, role/group-based authorization, failure handling, and audit logging.
