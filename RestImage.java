package com.valuephone.image.management.images;

/**
 * Created with IntelliJ IDEA.
 * <p>
 * User: ahack Date: 29.09.17 Time: 11:25
 */
public class RestImage {
    public final String mimeType;
    public final byte[] imageData;

    public RestImage(String mime, byte[] imageData) {
        this.mimeType = mime;
        this.imageData = imageData;
    }
}
