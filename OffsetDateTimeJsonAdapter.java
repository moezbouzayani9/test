package com.valuephone.image.helper;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.valuephone.image.utilities.DateTimeUtilities;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.OffsetDateTime;

@Slf4j
public class OffsetDateTimeJsonAdapter extends TypeAdapter<OffsetDateTime> {

    @Override
    public void write(JsonWriter out, OffsetDateTime value) throws IOException {
        if(value != null) {
            out.value(DateTimeUtilities.formatOffsetDateTime(value));
        } else{
            out.nullValue();
        }
    }

    @Override
    public OffsetDateTime read(JsonReader in) throws IOException {
            String dateString = in.hasNext() ? in.nextString() : null;
            return DateTimeUtilities.parseOffsetTimestamp(dateString);
    }
}
