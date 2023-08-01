package com.valuephone.image.helper;

import com.valuephone.image.exception.PreconditionException;

public class Reject {
    public static void ifNull(Object object, String message) {
        if (object == null)
            throw new PreconditionException(message);
    }

    public static void ifTrue(boolean condition, String message) {
        if (condition)
            throw new PreconditionException(message);
    }

    public static void ifFalse(boolean condition, String message) {
        if (!condition)
            throw new PreconditionException(message);
    }
}
