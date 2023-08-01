package com.valuephone.image.dto;

import java.time.LocalDateTime;

/**
 * @author tcigler
 * @since 1.0
 */
public class ImageMetadata {

    protected final long imageId;
    protected final ImageMimeType mimeType;
    protected final String poolId;
    protected final LocalDateTime validFrom;
    protected final LocalDateTime validTill;
    protected int width;
    protected int height;

    public ImageMetadata(final long imageId, final String poolId, final ImageMimeType mimeType, final int width, final int height, final LocalDateTime validFrom, final LocalDateTime validTill) {
        this.mimeType = mimeType;
        this.width = width;
        this.height = height;
        this.poolId = poolId;
        this.validFrom = validFrom;
        this.validTill = validTill;
        this.imageId = imageId;
    }

    public long getImageId() {
        return imageId;
    }

    public String getPoolId() {
        return poolId;
    }

    public ImageMimeType getMimeType() {
        return mimeType;
    }

    public int getWidth() {
        return width;
    }

    protected void setWidth(final int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    protected void setHeight(final int height) {
        this.height = height;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public LocalDateTime getValidTill() {
        return validTill;
    }

    @Override
    public String toString() {
        return "ImageMetadata{" +
                "imageId=" + imageId +
                ", mimeType=" + mimeType +
                ", width=" + width +
                ", height=" + height +
                ", poolId='" + poolId + '\'' +
                ", validFrom=" + validFrom +
                ", validTill=" + validTill +
                '}';
    }
}
