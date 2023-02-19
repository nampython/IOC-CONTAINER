package org.ioc.exception;

public class AlreadyInitializedException extends RuntimeException {
    public AlreadyInitializedException(String message) {
        super(message);
    }

    public AlreadyInitializedException(String message, Throwable cause) {
        super(message, cause);
    }
}
