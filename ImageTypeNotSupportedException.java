package com.valuephone.image.exception;

/**
 * @author tcigler
 * @since 1.0
 */
public class ImageTypeNotSupportedException extends ImageException {

    private static final long serialVersionUID = 1737480350060841268L;


    public ImageTypeNotSupportedException(final String message, final Object... messageParameters) {
        super(message, messageParameters);
    }

    public ImageTypeNotSupportedException(final Throwable cause) {
        super(cause);
    }

    public ImageTypeNotSupportedException(final Throwable cause, final String message, final Object... messageParameters) {
        super(cause, message, messageParameters);
    }

}
