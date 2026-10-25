package com.foody.exception;

import org.springframework.http.HttpStatus;

/** Request is invalid (HTTP BAD_REQUEST). */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
