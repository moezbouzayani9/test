package com.valuephone.image.management.images;

import com.valuephone.image.helper.Reject;

/**
 * Created with IntelliJ IDEA.
 * <p>
 * User: ahack Date: 30.05.16 Time: 15:28
 */
public class ImageLifetimeKey {

    public final Long deduplicatedImageId;

    public ImageLifetimeKey(Long deduplicatedImageId) {
        Reject.ifNull(deduplicatedImageId, "no deduplicated image id");

        this.deduplicatedImageId = deduplicatedImageId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ImageLifetimeKey))
            return false;

        ImageLifetimeKey that = (ImageLifetimeKey) o;

        return deduplicatedImageId.equals(that.deduplicatedImageId);

    }

    @Override
    public int hashCode() {
        int result = deduplicatedImageId.hashCode();
        result = 31 * result;
        return result;
    }

    @Override
    public String toString() {
        return "BoImageLifetimeKey{" +
                "deduplicatedImageId=" + deduplicatedImageId +
                '}';
    }
}
