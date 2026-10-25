package com.foody.exception;

import org.springframework.http.HttpStatus;

/** Authentication failed (HTTP UNAUTHORIZED). */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
