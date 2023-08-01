package com.valuephone.image.exception;

public class ImageContentHasXSSException extends ImageException {

    public ImageContentHasXSSException(final String message, final Object... messageParameters) {
        super(message, messageParameters);
    }
}
