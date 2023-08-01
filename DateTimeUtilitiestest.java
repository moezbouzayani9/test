package com.valuephone.image.utilities;

import com.valuephone.image.management.images.jdbc.OrderedIndexedNullableStatement;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

@Slf4j
public class DateTimeUtilitiestest {

    static final String [] inputDates = {
            "2022-09-26T14:45:52,Europa/Berlin",
            "2022-09-26T14:45:52, Europa/Berlin",
            "2022-09-26T14:45:52,",
            "2022-09-26",
            "2022-09-26T14:45:52",
            "2022-09-26T14:45:52+03",
            "2022-09-26T14:45:52+04:00",
            "2022-02-25 15:45:01.068+00",
            "2022-02-02 14:00:25",
            "2022-02-02 14:00:04",
            "2050-12-31 23:59:59",
            "1970-01-01 00:00:00",
            "2051-01-30"
            /*"2022-09-26T14:45:52+0200",*/
    };



    @Test
    void formatParseTest(){
        for (String inputDate : inputDates) {
            OffsetDateTime offsetDateTime = DateTimeUtilities.parseOffsetTimestamp(inputDate);

            String jdbcFormat = OrderedIndexedNullableStatement.formatZonedTimestamp(offsetDateTime.toZonedDateTime());
            log.info("jdbc format: " + jdbcFormat);

            assert !jdbcFormat.contains("CEST") && !jdbcFormat.contains("CET");

            log.info("jdbc parsed " + DateTimeUtilities.parseOffsetTimestamp(jdbcFormat));

            String date = DateTimeUtilities.formatOffsetDateTime(offsetDateTime);
            log.info(date);
        }
    }
}
