package com.valuephone.image.helper;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
public class DateJsonAdapter extends TypeAdapter<Date> {

    @Override
    public void write(JsonWriter out, Date value) throws IOException {
        if(value != null) {
            out.value(value.toLocalDate().format(DateTimeFormatter.ISO_DATE));
        } else{
            out.nullValue();
        }
    }

    @Override
    public Date read(JsonReader in) throws IOException {
            String dateString = in.hasNext() ? in.nextString() : null;

            return Optional.ofNullable(dateString)
                    .map(DateTimeFormatter.ISO_DATE::parse)
                    .map(LocalDate::from)
                    .map(Date::valueOf)
                    .orElse(null);
    }
}
