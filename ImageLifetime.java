package com.valuephone.image.management.images;


import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Created with IntelliJ IDEA.
 * <p>
 * User: ahack Date: 27.05.16 Time: 11:38
 */
public class ImageLifetime {

    public final OffsetDateTime validFrom;
    public final OffsetDateTime validTill;

    public final boolean isMissingLifetime;

    public ImageLifetime(OffsetDateTime validFrom, OffsetDateTime validTill) {
        this.validFrom = validFrom;
        this.validTill = validTill;

        this.isMissingLifetime = false;
    }

    public ImageLifetime(Timestamp validFrom, ZoneId validFromZoneId, Timestamp validTill, ZoneId validTillZoneId) {
        this.validFrom = validFrom != null ?
                OffsetDateTime.ofInstant(validFrom.toInstant(), validFromZoneId) : null;
        this.validTill = validTill != null ?
                OffsetDateTime.ofInstant(validTill.toInstant(), validTillZoneId) : null;

        this.isMissingLifetime = false;
    }

    public ImageLifetime() {
        this.validFrom = null;
        this.validTill = null;
        this.isMissingLifetime = true;
    }

    public boolean isOutdatedLifetime() {
        return validTill != null && validTill.isBefore(OffsetDateTime.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ImageLifetime))
            return false;

        ImageLifetime that = (ImageLifetime) o;

        if (isMissingLifetime != that.isMissingLifetime)
            return false;
        if (validFrom != null ? !validFrom.equals(that.validFrom) : that.validFrom != null)
            return false;
        return validTill != null ? validTill.equals(that.validTill) : that.validTill == null;

    }

    @Override
    public int hashCode() {
        int result = validFrom != null ? validFrom.hashCode() : 0;
        result = 31 * result + (validTill != null ? validTill.hashCode() : 0);
        result = 31 * result + (isMissingLifetime ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BoImageLifetime{" +
                "validFrom=" + validFrom +
                ", validTill=" + validTill +
                ", isMissingLifetime=" + isMissingLifetime +
                '}';
    }
}
