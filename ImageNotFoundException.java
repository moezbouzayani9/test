package com.valuephone.image.exception;

/**
 * @author tcigler
 * @since 1.0
 */
public class ImageNotFoundException extends ImageException {

    private static final long serialVersionUID = -7172775428059035244L;

    public ImageNotFoundException() {
        this("Image was not found");
    }

    public ImageNotFoundException(String message, Object... messageParameters) {
        super(message, messageParameters);
    }

    public ImageNotFoundException(final long imageId, String poolId) {
        super("Image id=" + imageId + " poolId=" + poolId + " was not found");
    }

    public ImageNotFoundException(final long imageId) {
        super("Image id=" + imageId + " was not found");
    }
}
