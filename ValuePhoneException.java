package com.valuephone.image.exception;

public class ValuePhoneException extends Exception {
    public ValuePhoneException(String message, Object... messageParameters) {
        super(messageParameters != null && messageParameters.length > 0 ? String.format(message, messageParameters)
                : message);
    }

    public ValuePhoneException(Throwable cause) {
        super(cause);
    }

    public ValuePhoneException(Throwable cause, String message, Object... messageParameters) {
        super(messageParameters != null && messageParameters.length > 0 ? String.format(message, messageParameters)
                : message, cause);
    }
}
