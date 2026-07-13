# Feature Spec: Rate Limit the Step-Up MFA Endpoint

Status: Draft

## Overview
Users can currently call `POST /auth/step-up/verify` an unlimited number of
times per minute. This allows brute-forcing the MFA challenge code. We need to
limit how many verification attempts a single user can make in a time window.

## User story
As a system operator, I want repeated failed MFA attempts from the same user
to be throttled, so that brute-forcing a step-up challenge code is impractical.

## Functional requirements
- FR1: A user may attempt `/auth/step-up/verify` at most 5 times per rolling
  60-second window.
- FR2: The 6th and subsequent attempts within the window MUST be rejected
  with HTTP 429, without invoking the MFA challenge service at all.
- FR3: A rejected (rate-limited) attempt MUST NOT reset or extend the window.
- FR4: Rate-limit state is tracked per username, not globally.
- FR5: A successful verification does not reset the counter early — the
  window is time-based only, not attempt-based.

## Non-functional requirements
- NFR1: No new external dependency (no Redis) — in-memory is acceptable,
  consistent with existing session/audit storage, and must be documented as
  such in the Known Limitations section of the README.
- NFR2: Thread-safe under concurrent requests for the same user.

## Success criteria
- SC1: An automated test proves the 6th attempt within 60 seconds is rejected
  with 429.
- SC2: An automated test proves that after the window elapses, attempts are
  allowed again.
- SC3: A rate-limited response includes a `Retry-After` header.

## Out of scope
- Rate limiting other endpoints (login, session, audit) — tracked separately.
- Distributed/multi-instance rate limiting.

## Open questions
- [NEEDS CLARIFICATION] Should rate-limit rejections be audited the same way
  as MFA_FAILED events, or as a distinct outcome type?