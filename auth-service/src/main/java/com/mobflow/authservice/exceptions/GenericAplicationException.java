package com.mobflow.authservice.exceptions;

import com.mobflow.authservice.model.enums.ErrorTP;

public class GenericAplicationException extends RuntimeException {
    public GenericAplicationException(ErrorTP error) {
        super(String.valueOf(error));
    }
}
