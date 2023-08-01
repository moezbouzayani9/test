package com.valuephone.image.management.images;

import java.time.OffsetDateTime;

/**
 * Created with IntelliJ IDEA.
 * <p>
 * User: ahack Date: 20.05.16 Time: 09:28
 */
public class ImageDownloadResult {
    final byte[] imageData;
    final OffsetDateTime modificationDate;

    ImageDownloadResult(byte[] imageData, OffsetDateTime modificationDate) {
        this.imageData = imageData;
        this.modificationDate = modificationDate;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public OffsetDateTime getModificationDate() {
        return modificationDate;
    }
}
