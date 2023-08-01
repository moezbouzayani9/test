package com.valuephone.image.exception;

/**
 * @author tcigler
 * @since 1.0
 */
public class ImageRuntimeException extends Exception {

    private static final long serialVersionUID = 6840341600113304303L;

    public ImageRuntimeException() {
        super();
    }

    public ImageRuntimeException(String message, Object... messageParameters) {
        super(messageParameters != null && messageParameters.length > 0 ? String.format(message, messageParameters) : message);
    }

    public ImageRuntimeException(final Throwable cause) {
        super(cause);
    }

    protected ImageRuntimeException(Throwable cause, String message, Object... messageParameters) {
        super(messageParameters != null && messageParameters.length > 0 ? String.format(message, messageParameters) : message, cause);
    }
}
