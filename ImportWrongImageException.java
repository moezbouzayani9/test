package com.valuephone.image.exception;

public class ImportWrongImageException extends Exception {

    public ImportWrongImageException(String message) {
        super(message);
    }

    public ImportWrongImageException(final Throwable cause) {
        super(cause);
    }

    public ImportWrongImageException(String message, Throwable cause) {
        super(message, cause);
    }

}
