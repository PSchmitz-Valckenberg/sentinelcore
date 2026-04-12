package com.sentinelcore.exception;

public class InvalidCaseException extends RuntimeException {
    public InvalidCaseException(String message) {
        super(message);
    }

    public InvalidCaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
