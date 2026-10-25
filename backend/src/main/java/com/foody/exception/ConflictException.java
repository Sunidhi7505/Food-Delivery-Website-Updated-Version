package com.foody.exception;

import org.springframework.http.HttpStatus;

/** Resource already exists (HTTP CONFLICT). */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
