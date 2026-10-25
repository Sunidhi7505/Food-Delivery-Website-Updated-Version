package com.foody.exception;

import org.springframework.http.HttpStatus;

/** Requested resource does not exist (HTTP NOT_FOUND). */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
