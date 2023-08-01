package com.valuephone.image.helper;

import java.awt.image.BufferedImage;

/**
 * @author Tomas Cigler
 */
public class ImageSize {

    int imWidth;
    int imHeight;

    public ImageSize(BufferedImage image) {
        this.imWidth = image.getWidth();
        this.imHeight = image.getHeight();
    }

    public ImageSize(final int imWidth, final int imHeight) {
        this.imWidth = imWidth;
        this.imHeight = imHeight;
    }

    public int getWidth() {
        return imWidth;
    }

    public int getHeight() {
        return imHeight;
    }

    @Override
    public String toString() {
        return "ImageSize{" +
                "width=" + imWidth +
                ", height=" + imHeight +
                '}';
    }
}
