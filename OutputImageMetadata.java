package com.valuephone.image.dto;

import java.time.LocalDateTime;

/**
 * @author tcigler
 */
public class OutputImageMetadata extends ImageMetadata {

    private final long id;
    private final long binaryDataId;

    public OutputImageMetadata(final long id, final long binaryDataId, final long imageId, final ImageMimeType mimeType, final int width, final int height, final String poolId, final LocalDateTime validFrom, final LocalDateTime validTill) {
        super(imageId, poolId, mimeType, width, height, validFrom, validTill);
        this.id = id;
        this.binaryDataId = binaryDataId;
    }

    public long getId() {
        return id;
    }

    public long getBinaryDataId() {
        return binaryDataId;
    }


    @Override
    public String toString() {
        return "OutputImageMetadata{" +
                "id=" + id +
                ", binaryDataId=" + binaryDataId +
                ", imageId=" + imageId +
                ", mimeType=" + mimeType +
                ", poolId='" + poolId + '\'' +
                ", validFrom=" + validFrom +
                ", validTill=" + validTill +
                ", width=" + width +
                ", height=" + height +
                '}';
    }

}
