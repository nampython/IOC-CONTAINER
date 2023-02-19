package org.ioc.exception;

import org.ioc.stereotype.Component;

public class ComponentInstantiationException extends RuntimeException {

    public ComponentInstantiationException(String message) {
        super(message);
    }

    public ComponentInstantiationException(String message, Throwable cause) {
        super(message, cause);
    }
}
