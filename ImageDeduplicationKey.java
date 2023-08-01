package com.valuephone.image.management.images;

import com.google.common.hash.HashCode;
import com.valuephone.image.helper.Reject;
import com.valuephone.image.management.share.images.ImageType;

/**
 * Created with IntelliJ IDEA.
 * <p>
 * User: ahack Date: 18.03.16 Time: 10:54
 */
public class ImageDeduplicationKey {

    public final Integer imageSize;
    public final HashCode imageHash;
    public final ImageType imageType;

    public ImageDeduplicationKey(Integer imageSize, HashCode imageHash, ImageType imageType) {
        Reject.ifNull(imageSize, "no image size");
        Reject.ifNull(imageHash, "no image hash");
        imageType = imageType == null ? ImageType.UNKNOWN : imageType;

        this.imageSize = imageSize;
        this.imageHash = imageHash;
        this.imageType = imageType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ImageDeduplicationKey))
            return false;

        ImageDeduplicationKey that = (ImageDeduplicationKey) o;

        if (!imageSize.equals(that.imageSize))
            return false;
        if (!imageHash.equals(that.imageHash))
            return false;
        return imageType == that.imageType;

    }

    @Override
    public int hashCode() {
        int result = imageSize.hashCode();
        result = 31 * result + imageHash.hashCode();
        result = 31 * result + imageType.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "BoImageDeduplicationKey{" +
                "imageSize=" + imageSize +
                ", imageHash=" + imageHash +
                ", imageType=" + imageType +
                '}';
    }
}
