package com.valuephone.image.utilities;

import com.valuephone.image.management.images.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;

import java.io.IOException;

@Slf4j
public class SimpleImageInfoUtilities {

    public static ImageInfo getImageInfo(byte[] bytes) throws IOException {
        try {
            org.apache.commons.imaging.ImageInfo imageInfo = Imaging.getImageInfo(bytes);
            return new ImageInfo(imageInfo.getWidth(), imageInfo.getHeight(), imageInfo.getMimeType());
        } catch (ImageReadException e) {
            log.warn(e.getMessage(), e);
            throw new IOException(e.getMessage());
        }
    }
}
