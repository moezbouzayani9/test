package com.valuephone.image.exception;

/**
 * @author tcigler
 * @since 1.0
 */
public class ImageException extends ValuePhoneException {

    private static final long serialVersionUID = 1103074744258514063L;

    public ImageException(String message, Object... messageParameters) {
        super(message, messageParameters);
    }

    public ImageException(final Throwable cause) {
        super(cause);
    }

    public ImageException(Throwable cause, String message, Object... messageParameters) {
        super(cause, message, messageParameters);
    }
}
