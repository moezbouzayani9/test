package com.valuephone.image.exception;

public class FormatedIllegalArgumentException extends IllegalArgumentException {

    private static final long serialVersionUID = -7009772869754340735L;

    public FormatedIllegalArgumentException(String message, Object... messageParameters) {
        super(messageParameters != null && messageParameters.length > 0 ? String.format(message, messageParameters) : message);
    }

    public FormatedIllegalArgumentException(Throwable cause) {
        super(cause);
    }

    public FormatedIllegalArgumentException(Throwable cause, String message, Object... messageParameters) {
        super(messageParameters != null && messageParameters.length > 0 ? String.format(message, messageParameters) : message, cause);
    }
}
