package com.edupulse.exception;

/** A password-reset (or similar one-time) token is invalid, expired or already used. Mapped to HTTP 400. */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
