package com.valuephone.image.exception;

@SuppressWarnings("serial")
public class PreconditionException extends RuntimeException {
    public PreconditionException() {
        super();
    }

    public PreconditionException(String string) {
        super(string);
    }
}
