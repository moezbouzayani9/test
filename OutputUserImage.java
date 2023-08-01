package com.valuephone.image.dto;

/**
 * @author tcigler
 * @since 1.0
 */
public class OutputUserImage {

    private final byte[] imageBytes;
    private final ImageMimeType mimeType;
    private final boolean publiclyVisible;

    public OutputUserImage(final byte[] imageBytes, final ImageMimeType mimeType, final boolean publiclyVisible) {
        this.imageBytes = imageBytes;
        this.mimeType = mimeType;
        this.publiclyVisible = publiclyVisible;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public ImageMimeType getMimeType() {
        return mimeType;
    }

    public boolean isPubliclyVisible() {
        return publiclyVisible;
    }
}
