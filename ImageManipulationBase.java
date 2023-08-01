package com.valuephone.image.helper;

import com.valuephone.image.dto.ImageMimeType;
import com.valuephone.image.exception.ImageException;
import com.valuephone.image.exception.ImageTypeNotSupportedException;
import com.valuephone.image.utilities.CheckUtilities;
import com.valuephone.image.utilities.SecurityUtilities;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

import static com.valuephone.image.helper.ImageManipulationManager.resize;

/**
 * @author tcigler
 * @since 1.0
 */
@Slf4j
public abstract class ImageManipulationBase /* implements AutoCloseable */ {

    /**
     * Constant quality that will be used for image output
     * Note: negotiated with authorities after comparing size/quality of output
     * (ImageIO default is around 0.75F, so bigger quality was chosen)
     */
    private static final float DEFAULT_QUALITY = 0.84F;

    private final ImageMimeType mimeType;
    byte[] imageBytes;
    byte[] resizedImageBytes;
    BufferedImage resizedImage;
    private BufferedImage bufferedImage;

    static {
        // turn off disk cache when manipulating image
        ImageIO.setUseCache(false);
    }

    protected ImageManipulationBase(final BufferedImage bufferedImage, final ImageMimeType mimeType) {
        CheckUtilities.checkArgumentNotNull(bufferedImage, "image");
        CheckUtilities.checkArgumentNotNull(mimeType, "mimeType");

        this.bufferedImage = bufferedImage;
        this.mimeType = mimeType;
    }

    public void resizeTo(Integer width, Integer height) {

        final ImageSize size = new ImageSize(bufferedImage);

        if (resize(size, width, height)) {
            resizedImage = resizeImageStraight(bufferedImage, mimeType, size.imWidth, size.imHeight);
            resizedImageBytes = null;
        } else {
            if (resizedImage != null) {
                resizedImage.flush();
                resizedImage = null;
            }
            resizedImageBytes = null;
        }

    }

    /**
     * Resizes image
     * returns new instance of buffered image
     * <p>
     * perserves original image - it's good to call BufferedImage.flush() on original if not needed anymore
     *
     * @param original
     * @param mimeType
     * @param width
     * @param height
     * @return
     */
    protected BufferedImage resizeImageStraight(BufferedImage original, ImageMimeType mimeType, int width, int height) {

        log.debug("Resizing image from {}x{} to {}x{}", original.getWidth(), original.getHeight(), width, height);

        long startTime = System.currentTimeMillis();

        final BufferedImage newImage = new BufferedImage(width, height, mimeType == ImageMimeType.PNG ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = newImage.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;

        log.debug("Image resize lasted: {} ms", elapsedTime);

        return newImage;
    }

    /**
     * Converts JPG image to RGB color space
     * returns new instance of buffered image
     * <p>
     * perserves original image - it's good to call BufferedImage.flush() on original if not needed anymore
     *
     * @param original
     * @return
     * @throws ImageException
     */
    public BufferedImage convertImagetoRGB(BufferedImage original) {
        log.debug("Converting image to RGB color space (by resizing)");
        return resizeImageStraight(original, ImageMimeType.JPG, original.getWidth(), original.getHeight());
    }

    /**
     * Gets correct image writer by mime type
     *
     * @param mimeType
     * @return
     * @throws ImageTypeNotSupportedException
     */
    public ImageWriter getImageWriter(ImageMimeType mimeType) throws ImageTypeNotSupportedException {
        Iterator<?> iter = ImageIO.getImageWritersByMIMEType(mimeType.toString());
        ImageWriter writer = (ImageWriter) iter.next();
        if (writer == null) {
            throw new ImageTypeNotSupportedException(mimeType.toString());
        }
        return writer;
    }

    public byte[] convertImageToBytes(BufferedImage image, ImageMimeType mimeType) throws ImageException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             MemoryCacheImageOutputStream memCache = new MemoryCacheImageOutputStream(byteArrayOutputStream)) {

            writeImageToOutputStream(image, mimeType, memCache);

            byte[] bytes = byteArrayOutputStream.toByteArray();

            SecurityUtilities.checkImageContentFromXSS(bytes);

            return bytes;

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new ImageException("Cannot convert image to bytes");
        }
    }

    public void writeImageToOutputStream(BufferedImage image, ImageMimeType mimeType, ImageOutputStream outputStream) throws IOException, ImageTypeNotSupportedException {
        final ImageWriter writer = getImageWriter(mimeType);

        try {
            writer.setOutput(outputStream);

            if (mimeType.isLossless()) {
                writer.write(image);
            } else { // Can change quality only on lossy image
                ImageWriteParam iwp = writer.getDefaultWriteParam();
                iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                iwp.setCompressionQuality(DEFAULT_QUALITY);

                IIOImage newImage = new IIOImage(image, null, null);
                writer.write(null, newImage, iwp);

            }

        } finally {
            writer.dispose();
        }

    }

    /**
     * Gets original image bytes or the new byte array interpretation of resized image or original provided image (from stream)
     *
     * @return
     * @throws ImageException
     */
    public byte[] getImageBytes() throws ImageException {

        if (resizedImageBytes != null) {
            return resizedImageBytes;
        }

        if (resizedImage != null) {
            resizedImageBytes = convertImageToBytes(resizedImage, mimeType);
            return resizedImageBytes;
        }

        if (imageBytes == null || needsReparation(bufferedImage)) {
            imageBytes = convertImageToBytes(getOriginalImageRepairedIfNecessary(), mimeType);
        }

        return imageBytes;

    }

    BufferedImage getOriginalImageRepairedIfNecessary() {

        // When the type is "strange" (problematic images that comes from retailers) - known combination
        if (needsReparation(bufferedImage)) {
            final BufferedImage temp = convertImagetoRGB(bufferedImage);
            bufferedImage.flush();

            // original bytes are no longer valid
            imageBytes = null;
            bufferedImage = temp;
        }

        return bufferedImage;
    }

    boolean needsReparation(final BufferedImage image) {
        return mimeType == ImageMimeType.JPG && image.getType() == BufferedImage.TYPE_CUSTOM;
    }

    public BufferedImage getBufferedImage() {
        return Optional.ofNullable(resizedImage)
                // when the image was not modified (by resizing), check lazily if needs to be repaired
                .orElseGet(this::getOriginalImageRepairedIfNecessary);
    }

    public ImageMimeType getMimeType() {
        return mimeType;
    }

    public int getWidth() {
        return Optional.ofNullable(resizedImage).map(BufferedImage::getWidth).orElse(bufferedImage.getWidth());
    }

    public int getHeight() {
        return Optional.ofNullable(resizedImage).map(BufferedImage::getHeight).orElse(bufferedImage.getHeight());
    }


}
