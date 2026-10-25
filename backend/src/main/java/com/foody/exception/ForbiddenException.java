package com.foody.exception;

import org.springframework.http.HttpStatus;

/** Action not allowed (HTTP FORBIDDEN). */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
