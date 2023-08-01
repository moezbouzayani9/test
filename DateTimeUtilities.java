package com.valuephone.image.utilities;

import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import lombok.extern.slf4j.Slf4j;

import java.text.ParsePosition;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Class DateTimeUtils
 *
 * @author Tomas Cigler
 * @since 1.0
 */
@Slf4j
public class DateTimeUtilities {
    final static DateTimeFormatter HTTP_HEADER_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz").withLocale(Locale.ENGLISH);

    private DateTimeUtilities() {
    }

    public static LocalDateTime getDateTimeFromHttpHeaderDateString(String headerDate) {
        return LocalDateTime.parse(headerDate, HTTP_HEADER_DATE_FORMATTER);
    }

    public static String formatOffsetDateTime(OffsetDateTime value){
        return value == null ? null : value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public static OffsetDateTime parseOffsetTimestamp(String input)  {
        if(input == null || input.trim().isEmpty() || "null".equalsIgnoreCase(input)){
            log.warn("offsetTimestamp is empty/null");
            return null;
        }
        input=input.replaceAll("[()\"]","").trim().replaceAll("\\s+","T");
        String[] split = input.split(",");
        if(split.length > 0){
            input = split[0];
        }
        String zone;
        if (split.length > 1) {
            zone = split[1];
        }else{
            zone = ZoneId.systemDefault().toString();
        }

        if(input.indexOf('T') < 0){
            input += 'T' + LocalTime.of(0,0,0,0).format(DateTimeFormatter.ISO_LOCAL_TIME);
        }

        int offsetIndex;
        if((offsetIndex = input.indexOf('+')) < 0){
            input = input + OffsetDateTime.now().getOffset();
        }else {

            String offset = input.substring(offsetIndex);
            if (offset.length() == 3) {
                offset += ":00";
            }
            input = input.substring(0, offsetIndex)+offset;
        }

        ParsePosition pos = new ParsePosition(0);
        OffsetDateTime retVal = OffsetDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.withResolverStyle(ResolverStyle.SMART).parse(input, pos)).atZoneSameInstant(ZoneId.of(zone)).toOffsetDateTime();

        if (pos.getErrorIndex() != -1 || retVal == null) {
            throw new IllegalStateException("can't parse date at POS: " + pos.getErrorIndex() + " of a string: " + input);
        }


        return retVal;
    }

    public static boolean isIfModifiedHeaderPresentAndActual(HeaderMap headers, final LocalDateTime imageValidFrom) {
        try {
            String modifiedSinceHeaderDate = headers.getFirst(Headers.IF_MODIFIED_SINCE);
            if (modifiedSinceHeaderDate != null) {
                final LocalDateTime validFromWithoutMilis = imageValidFrom.truncatedTo(ChronoUnit.SECONDS);
                final LocalDateTime ifModifiedDateTime = DateTimeUtilities.getDateTimeFromHttpHeaderDateString(modifiedSinceHeaderDate);
                return !validFromWithoutMilis.isAfter(ifModifiedDateTime);
            }
        } catch (Exception ignore) { // not a required behaviour
        }

        return false;
    }

    /**
     * Check if the image should be currently visible and not expired
     *
     * @param dateFrom
     * @param dateTill
     */
    public static boolean isPeriodValid(LocalDateTime dateFrom, LocalDateTime dateTill) {
        final LocalDateTime currentDateTime = LocalDateTime.now();
        return (currentDateTime.isBefore(dateTill)); // FIXME: currentDateTime.isAfter(dateFrom) && removed as a hotfix for 6.9.0 version!
    }

}
