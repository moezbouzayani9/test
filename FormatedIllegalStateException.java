package com.valuephone.image.exception;

public class FormatedIllegalStateException extends IllegalStateException {

    private static final long serialVersionUID = 241933135078046035L;

    public FormatedIllegalStateException(String message, Object... messageParameters) {
        super(messageParameters != null && messageParameters.length > 0 ? String.format(message, messageParameters) : message);
    }

    public FormatedIllegalStateException(Throwable cause) {
        super(cause);
    }

    public FormatedIllegalStateException(Throwable cause, String message, Object... messageParameters) {
        super(messageParameters != null && messageParameters.length > 0 ? String.format(message, messageParameters) : message, cause);
    }
}
