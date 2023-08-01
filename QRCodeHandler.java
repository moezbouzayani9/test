package com.valuephone.image.handler;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.valuephone.image.dto.ImageMimeType;
import com.valuephone.image.utilities.CheckUtilities;
import com.valuephone.image.utilities.Constants;
import com.valuephone.image.utilities.HandlerUtilities;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.util.Deque;
import java.util.Map;

import static com.valuephone.image.utilities.HandlerUtilities.readIntegerParam;
import static com.valuephone.image.utilities.HandlerUtilities.readStringParam;

/**
 * @author tcigler
 * @since 1.0
 */
@Slf4j
public class QRCodeHandler implements HttpHandler {

    private static final ImageMimeType QRCODE_MIME_TYPE = ImageMimeType.JPG;

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        final Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        String text = readStringParam(queryParameters, Constants.QR_TEXT_PARAMETER);
        Integer requestedWidth = readIntegerParam(queryParameters, Constants.WIDTH_PARAMETER);
        Integer requestedHeight = readIntegerParam(queryParameters, Constants.HEIGHT_PARAMETER);

        CheckUtilities.checkStringArgumentNotEmpty(text, Constants.QR_TEXT_PARAMETER);

        int width = checkAndGetSize(requestedWidth, Constants.WIDTH_PARAMETER);
        int height = checkAndGetSize(requestedHeight, Constants.HEIGHT_PARAMETER);

        // Encode.forHtml(text) - use or not?
        final BufferedImage generatedQRCode = generateQRCode(text, width, height);

        HandlerUtilities.sendImageAsResponse(exchange, generatedQRCode, QRCODE_MIME_TYPE);

    }

    /**
     * Generates QR code based on text with defined width and height
     *
     * @param text   not null string without diacritic
     * @param width
     * @param height
     * @return
     */
    public static BufferedImage generateQRCode(String text, int width, int height) {
        log.debug("Generating QR code for text: {}", text);

        try {
            QRCodeWriter qrWriter = new QRCodeWriter();
            BitMatrix matrix = qrWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

            width = matrix.getWidth();
            height = matrix.getHeight();

            log.debug("Generated QR code dimensions: width = {}, height = {}", width, height);

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    boolean grayValue = matrix.get(x, y);
                    image.setRGB(x, y, (grayValue ? 0 : 0xFFFFFF));
                }
            }

            return image;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException("Error occurred during QR code creation", e);
        }
    }

    private int checkAndGetSize(final Integer sizeParameter, final String paramName) {

        final int MAX_SIZE = 4096; // 4K resolution

        if (sizeParameter == null) {
            return 0;
        }

        if (sizeParameter < 0) {
            throw new IllegalArgumentException(paramName + " value cannot be negative");
        }

        if (sizeParameter > MAX_SIZE) {
            throw new IllegalArgumentException(paramName + " value cannot be more than " + MAX_SIZE);
        }

        return sizeParameter;
    }

}
