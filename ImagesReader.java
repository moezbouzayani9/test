package com.valuephone.image.management.share.images;

import org.apache.commons.imaging.ImageReadException;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * @author fmelan
 */
public interface ImagesReader {

    /**
     * Returns an image provided as an array of bytes as a Java BufferedImage.
     *
     * @param bytes
     * @return
     * @throws IOException
     * @throws ImageReadException
     */
    BufferedImage readImage(byte[] bytes) throws IOException, ImageReadException;

}
