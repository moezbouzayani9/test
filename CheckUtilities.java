package com.valuephone.image.utilities;

import com.valuephone.image.exception.FormatedIllegalArgumentException;

public final class CheckUtilities {

    private CheckUtilities() {
    }

    public static void checkArgumentNotNull(Object argument, String argumentName) throws IllegalArgumentException {

        checkArgumentName(argumentName);

        if (argument == null) {

            throw new FormatedIllegalArgumentException("Argument %s cannot be null!", argumentName);
        }
    }

    public static void checkStringArgumentNotEmpty(String argument, String argumentName) throws IllegalArgumentException {

        checkArgumentName(argumentName);

        if (argument == null) {

            throw new FormatedIllegalArgumentException("String argument %s cannot be null!", argumentName);
        }

        if (argument.isEmpty()) {

            throw new FormatedIllegalArgumentException("String argument %s cannot be empty!", argumentName);
        }
    }

    public static void checkPositiveArgument(long argument, String argumentName) throws IllegalArgumentException {

        checkArgumentName(argumentName);

        if (argument <= 0) {
            throw new FormatedIllegalArgumentException("Argument %s must be positive!", argumentName);
        }
    }

    private static void checkArgumentName(String argumentName) {

        if (argumentName == null) {
            throw new IllegalArgumentException("Argument name cannot be null!");
        }

        if (argumentName.trim().isEmpty()) {
            throw new IllegalArgumentException("Argument name cannot be empty!");
        }
    }
}
