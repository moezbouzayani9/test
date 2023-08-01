package com.valuephone.image.exception;

/**
 * @author tcigler
 * @since 1.0
 */
public class SecurityException extends ImageException {

    private static final long serialVersionUID = 4644362098143086776L;


    public SecurityException(final String message, final Object... messageParameters) {
        super(message, messageParameters);
    }

}
