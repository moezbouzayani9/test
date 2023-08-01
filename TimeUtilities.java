package com.valuephone.image.utilities;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public final class TimeUtilities {

    private TimeUtilities() {
    }

    public static LocalDateTime convertFromDateToLocalDateTime(Date date) {

        if (date == null) {
            return null;
        }

        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
