package com.rahulshukla.authengine.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldMapBadRequestExceptionsToApiError() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/flow");

        var response = handler.handleBadRequest(new IllegalArgumentException("bad input"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).isEqualTo("bad input");
        assertThat(response.getBody().path()).isEqualTo("/auth/flow");
        assertThat(response.getBody().timestamp()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void shouldMapUnexpectedExceptionsToServerError() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/error");

        var response = handler.handleUnexpected(new RuntimeException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("Unexpected server error");
        assertThat(response.getBody().path()).isEqualTo("/auth/error");
    }

    @Test
    void shouldExposeApiErrorRecordFields() {
        Instant now = Instant.parse("2026-07-13T20:00:00Z");
        ApiError error = new ApiError(now, 400, "Bad Request", "message", "/path");

        assertThat(error.timestamp()).isEqualTo(now);
        assertThat(error.status()).isEqualTo(400);
        assertThat(error.error()).isEqualTo("Bad Request");
        assertThat(error.message()).isEqualTo("message");
        assertThat(error.path()).isEqualTo("/path");
    }
}
