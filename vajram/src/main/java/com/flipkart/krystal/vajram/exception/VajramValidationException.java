package com.flipkart.krystal.vajram.exception;

import java.io.Serial;

public class VajramValidationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -7618868579700286025L;

    public VajramValidationException() {
        super();
    }

    public VajramValidationException(String message) {
        super(message);
    }

    public VajramValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public VajramValidationException(Throwable cause) {
        super(cause);
    }

    protected VajramValidationException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
