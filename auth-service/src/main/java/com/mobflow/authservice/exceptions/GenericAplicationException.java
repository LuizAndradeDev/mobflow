package com.mobflow.authservice.exceptions;

import com.mobflow.authservice.domain.model.enums.ErrorTP;

public class GenericAplicationException extends RuntimeException {
    public GenericAplicationException(ErrorTP error) {
        super(String.valueOf(error));
    }
}
