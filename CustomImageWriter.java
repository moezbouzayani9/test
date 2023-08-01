package com.valuephone.image.helper;


import com.valuephone.image.dto.ImageMimeType;
import com.valuephone.image.exception.ImageException;
import com.valuephone.image.exception.ImageTypeNotSupportedException;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Class CustomImageReader
 *
 * @author Tomas Cigler
 * @since 1.0
 */
public class CustomImageWriter extends ImageManipulationBase {

    static {
        // turn off disk cache when reading image
        ImageIO.setUseCache(false);
    }

    CustomImageWriter(CustomImageReader reader) {
        super(reader.getBufferedImage(), reader.getMimeType());
    }

    public CustomImageWriter(BufferedImage bufferedImage, final ImageMimeType mimeType)
            throws ImageException {
        super(bufferedImage, mimeType);
    }

    public void writeImage(OutputStream outputStream) throws IOException, ImageTypeNotSupportedException {

        try (MemoryCacheImageOutputStream memCache = new MemoryCacheImageOutputStream(outputStream)) {

            writeImageToOutputStream(getBufferedImage(), getMimeType(), memCache);

        }

    }

}
