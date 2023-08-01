package com.valuephone.image.management.share.images;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.bytesource.ByteSource;
import org.apache.commons.imaging.common.bytesource.ByteSourceArray;
import org.apache.commons.imaging.formats.jpeg.JpegImageParser;
import org.apache.commons.imaging.formats.jpeg.segments.Segment;
import org.apache.commons.imaging.formats.jpeg.segments.UnknownSegment;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

@Slf4j
public class ImagesReaderImpl implements ImagesReader {

    public static final int COLOR_TYPE_RGB = 1;
    public static final int COLOR_TYPE_CMYK = 2;
    public static final int COLOR_TYPE_YCCK = 3;
    public static final String ICC_PROFILE_FILENAME = "ISOcoated_v2_300_eci.icc";

    private int colorType = COLOR_TYPE_RGB;
    private boolean hasAdobeMarker = false;
    private static ICC_Profile defaultIccProfile = null;

    static {
        ImageIO.setUseCache(false);
        try (InputStream inputStream = ImagesReaderImpl.class.getResourceAsStream("/" + ICC_PROFILE_FILENAME)) {
            defaultIccProfile = ICC_Profile.getInstance(inputStream);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
            throw new IllegalStateException("Cannot load resource file " + ICC_PROFILE_FILENAME);
        }
    }

    @Override
    public BufferedImage readImage(byte[] bytes) throws IOException, ImageReadException {
        colorType = COLOR_TYPE_RGB;
        hasAdobeMarker = false;

        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> iter = ImageIO.getImageReaders(stream);
            if (iter.hasNext()) {
                ImageReader reader = iter.next();

                BufferedImage image;
                ICC_Profile profile = null;
                try {
                    reader.setInput(stream);
                    image = reader.read(0);
                } catch (IIOException e) {
                    colorType = COLOR_TYPE_CMYK;
                    checkAdobeMarker(bytes);
                    profile = Imaging.getICCProfile(bytes);
                    WritableRaster raster = (WritableRaster) reader.readRaster(0, null);
                    if (colorType == COLOR_TYPE_YCCK) {
                        convertYcckToCmyk(raster);
                    }
                    if (hasAdobeMarker) {
                        convertInvertedColors(raster);
                    }
                    image = convertCmykToRgb(raster, profile);
                } finally {
                    reader.dispose();
                }

                return image;
            }
        }

        return null;
    }

    private void checkAdobeMarker(byte[] bytes) throws IOException, ImageReadException {
        try {
            JpegImageParser parser = new JpegImageParser();
            ByteSource byteSource = new ByteSourceArray(bytes);
            List<Segment> segments = parser.readSegments(byteSource, new int[]{0xffee}, true);
            if (segments != null && segments.size() >= 1) {
                UnknownSegment app14Segment = (UnknownSegment) segments.get(0);
                byte[] data = app14Segment.getSegmentData();
                if (data.length >= 12 && data[0] == 'A' && data[1] == 'd' && data[2] == 'o' && data[3] == 'b'
                        && data[4] == 'e') {
                    hasAdobeMarker = true;
                    int transform = app14Segment.getSegmentData()[11] & 0xff;
                    if (transform == 2) {
                        colorType = COLOR_TYPE_YCCK;
                    }
                }
            }
        } catch (ImageReadException e) {
            hasAdobeMarker = false;
        }
    }

    private void convertYcckToCmyk(WritableRaster raster) {
        int height = raster.getHeight();
        int width = raster.getWidth();
        int stride = width * 4;
        int[] pixelRow = new int[stride];
        for (int h = 0; h < height; h++) {
            raster.getPixels(0, h, width, 1, pixelRow);

            for (int x = 0; x < stride; x += 4) {
                int y = pixelRow[x];
                int cb = pixelRow[x + 1];
                int cr = pixelRow[x + 2];

                int c = (int) (y + 1.402 * cr - 178.956);
                int m = (int) (y - 0.34414 * cb - 0.71414 * cr + 135.95984);
                y = (int) (y + 1.772 * cb - 226.316);

                if (c < 0) {
                    c = 0;
                } else if (c > 255) {
                    c = 255;
                }
                if (m < 0) {
                    m = 0;
                } else if (m > 255) {
                    m = 255;
                }
                if (y < 0) {
                    y = 0;
                } else if (y > 255) {
                    y = 255;
                }

                pixelRow[x] = 255 - c;
                pixelRow[x + 1] = 255 - m;
                pixelRow[x + 2] = 255 - y;
            }

            raster.setPixels(0, h, width, 1, pixelRow);
        }
    }

    private void convertInvertedColors(WritableRaster raster) {
        int height = raster.getHeight();
        int width = raster.getWidth();
        int stride = width * 4;
        int[] pixelRow = new int[stride];
        for (int h = 0; h < height; h++) {
            raster.getPixels(0, h, width, 1, pixelRow);
            for (int x = 0; x < stride; x++) {
                pixelRow[x] = 255 - pixelRow[x];
            }
            raster.setPixels(0, h, width, 1, pixelRow);
        }
    }

    private BufferedImage convertCmykToRgb(Raster cmykRaster, ICC_Profile cmykProfile) throws IOException {
        if (cmykProfile == null) {
            cmykProfile = defaultIccProfile;
        }
        ICC_ColorSpace cmykCS = new ICC_ColorSpace(cmykProfile);
        BufferedImage rgbImage = new BufferedImage(cmykRaster.getWidth(), cmykRaster.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        WritableRaster rgbRaster = rgbImage.getRaster();
        ColorSpace rgbCS = rgbImage.getColorModel().getColorSpace();
        ColorConvertOp cmykToRgb = new ColorConvertOp(cmykCS, rgbCS, null);
        cmykToRgb.filter(cmykRaster, rgbRaster);
        return rgbImage;
    }
}
