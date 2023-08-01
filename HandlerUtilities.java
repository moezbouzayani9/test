package com.valuephone.image.utilities;

import com.valuephone.image.dto.ImageMimeType;
import com.valuephone.image.exception.ImageException;
import com.valuephone.image.helper.CustomImageReader;
import com.valuephone.image.helper.CustomImageWriter;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ExceptionHandler;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author tcigler
 * @since 1.0
 */
@Slf4j
public class HandlerUtilities {

    private HandlerUtilities() {
    }

    /**
     * returns first found parameter as String
     *
     * @param queryParameters
     * @param parameterName
     * @return parameter value or null
     */
    public static String readStringParam(final Map<String, Deque<String>> queryParameters, final String parameterName) {
        return readParam(queryParameters, parameterName, s->s);
    }

    public static <T extends Enum<T>> T readEnumParam(final Map<String, Deque<String>> queryParameters, final String parameterName, Class<T> enumClass) {
        return readParam(queryParameters, parameterName, s->Enum.valueOf(enumClass,s));
    }

    /**
     * returns first found parameter as String
     *
     * @param queryParameters
     * @param parameterName
     * @return parameter value or null
     */
    public static OffsetDateTime readOffsetDateTimeParam(final Map<String, Deque<String>> queryParameters, final String parameterName) {
        return readParam(queryParameters, parameterName, DateTimeUtilities::parseOffsetTimestamp);
    }

    public static LocalDateTime readTimestampParam(final Map<String, Deque<String>> queryParameters, final String parameterName) {
        return readParam(queryParameters, parameterName, s->Optional.ofNullable(DateTimeUtilities.parseOffsetTimestamp(s)).map(OffsetDateTime::toLocalDateTime).orElse(null));
    }

    private static <T> T readParam(final Map<String, Deque<String>> queryParameters, final String parameterName, Function<String, T> mappingFunction){
        final Deque<String> param = queryParameters.get(parameterName);
        return Optional.ofNullable(param)
                .map(Deque::getFirst)
                .map(mappingFunction)
                .orElse(null);
    }

    /**
     * returns first found parameter, parsed to Integer, or null
     *
     * @param queryParameters
     * @param parameterName
     * @return parameter Integer value or null
     * @throws NumberFormatException in case of parameter does not contain valid Integer
     */
    public static Integer readIntegerParam(final Map<String, Deque<String>> queryParameters, final String parameterName) {
        return readParam(queryParameters, parameterName, Integer::parseInt);
    }

    public static Float readFloatParam(final Map<String, Deque<String>> queryParameters, final String parameterName) {
        return readParam(queryParameters, parameterName, Float::parseFloat);
    }

    /**
     * returns first found parameter, parsed to Long, or null
     *
     * @param queryParameters
     * @param parameterName
     * @return parameter Long value or null
     * @throws NumberFormatException in case of parameter does not contain valid Integer
     */
    public static Long readLongParam(final Map<String, Deque<String>> queryParameters, final String parameterName) {
        return readParam(queryParameters, parameterName, Long::parseLong);
    }


    /**
     * returns first found parameter, parsed to Boolean, or null
     *
     * @param queryParameters
     * @param parameterName
     * @return parameter Integer value or null
     * @throws NumberFormatException in case of parameter does not contain valid Integer
     */
    public static Boolean readBooleanParam(final Map<String, Deque<String>> queryParameters, final String parameterName) {
        return readParam(queryParameters, parameterName, Boolean::parseBoolean);
    }

    public static void sendImageAsResponse(final HttpServerExchange exchange, final byte[] binary, ImageMimeType mimeType, final Integer requestedWidth, final Integer requestedHeight) throws ImageException {

        final CustomImageReader customImageReader = new CustomImageReader(binary);

        final ImageMimeType detectedMimeType = customImageReader.getMimeType();

        if (detectedMimeType != mimeType) {
            log.warn("Provided mime type {} of image doesn't match detected ({})", mimeType, detectedMimeType);
        }

        customImageReader.resizeTo(requestedWidth, requestedHeight);

        final CustomImageWriter writer = customImageReader.toWriter();

        writeImageToResponseStream(exchange, writer);

    }

    public static void writeImageToResponseStream(final HttpServerExchange exchange, final CustomImageWriter writer) throws ImageException {
        exchange.startBlocking();
        try (OutputStream outputStream = exchange.getOutputStream()) {

            final HeaderMap responseHeaders = exchange.getResponseHeaders();

            responseHeaders.put(Headers.CONTENT_TYPE, writer.getMimeType().toString());

            writer.writeImage(outputStream);

            exchange.endExchange();

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new ImageException("Cannot write image to output");
        }
    }

    public static void sendImageAsResponse(final HttpServerExchange exchange, BufferedImage image, ImageMimeType mimeType) throws ImageException {
        sendImageAsResponse(exchange, image, mimeType, null, null);
    }

    public static void sendImageAsResponse(final HttpServerExchange exchange, BufferedImage image, ImageMimeType mimeType, final Integer requestedWidth, final Integer requestedHeight) throws ImageException {

        final CustomImageWriter writer = new CustomImageWriter(image, mimeType);

        writer.resizeTo(requestedWidth, requestedHeight);

        writeImageToResponseStream(exchange, writer);
    }

    public static void sendImageBytesAsResponse(final HttpServerExchange exchange, final ImageMimeType imageMimeType, final byte[] finalImageBytes) {

        exchange.setStatusCode(StatusCodes.OK);
        exchange.setResponseContentLength(finalImageBytes.length);
        final HeaderMap responseHeaders = exchange.getResponseHeaders();

        responseHeaders.put(Headers.CONTENT_TYPE, Objects.toString(imageMimeType));

        final Sender responseSender = exchange.getResponseSender();

        responseSender.send(ByteBuffer.wrap(finalImageBytes));

    }

    public static void handleSecurityException(final HttpServerExchange exchange) {

        final Throwable throwable = exchange.getAttachment(ExceptionHandler.THROWABLE);

        log.error(throwable.getMessage(), throwable);

        exchange.setStatusCode(StatusCodes.UNAUTHORIZED);
        exchange.getResponseSender().send(StatusCodes.UNAUTHORIZED_STRING);

    }

    public static void handleFatalException(final HttpServerExchange exchange) {
        final Throwable throwable = exchange.getAttachment(ExceptionHandler.THROWABLE);

        log.error(throwable.getMessage(), throwable);

        exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
        exchange.getResponseSender().send(StatusCodes.INTERNAL_SERVER_ERROR_STRING);
    }

    public static void handleNotFound(final HttpServerExchange exchange) {

        final Throwable throwable = exchange.getAttachment(ExceptionHandler.THROWABLE);

        final String message = throwable.getMessage();
        log.warn(message,throwable);

        exchange.setStatusCode(StatusCodes.NOT_FOUND);
        exchange.getResponseSender().send(message);
    }

    public static void handleBadRequest(HttpServerExchange exchange) {
        final Throwable throwable = exchange.getAttachment(ExceptionHandler.THROWABLE);

        log.warn("Wrong parameters were provided: {}", throwable.getMessage());
        log.debug("Wrong parameters were provided", throwable);

        exchange.setStatusCode(StatusCodes.BAD_REQUEST);
        exchange.getResponseSender().send(throwable.getMessage());
    }
}
