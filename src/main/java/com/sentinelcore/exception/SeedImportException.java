package com.sentinelcore.exception;

public class SeedImportException extends RuntimeException {

    public SeedImportException(String message) {
        super(message);
    }

    public SeedImportException(String message, Throwable cause) {
        super(message, cause);
    }
}