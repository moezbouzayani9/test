package com.valuephone.image.management.images.handler;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.valuephone.image.dto.ImageEntity;
import com.valuephone.image.dto.ImageMetadata;
import com.valuephone.image.dto.ImageMimeType;
import com.valuephone.image.exception.ImageException;
import com.valuephone.image.exception.ImageNotFoundException;
import com.valuephone.image.exception.ValuePhoneException;
import com.valuephone.image.handler.ImageModifyHandler;
import com.valuephone.image.handler.ImageUploadHandler;
import com.valuephone.image.helper.DateJsonAdapter;
import com.valuephone.image.helper.LocalDateJsonAdapter;
import com.valuephone.image.helper.OffsetDateTimeJsonAdapter;
import com.valuephone.image.helper.Reject;
import com.valuephone.image.management.images.*;
import com.valuephone.image.management.images.jdbc.ImageManagerJDBCUtills;
import com.valuephone.image.management.share.images.ImageType;
import com.valuephone.image.management.share.images.ImagesReader;
import com.valuephone.image.utilities.DatabaseManager;
import com.valuephone.image.utilities.SimpleImageInfoUtilities;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.imaging.ImageReadException;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class ImageManagerHanlderUtilities {
    private final DatabaseManager imageDatabaseManager;
    private final ImageRegistry imageRegistry;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeJsonAdapter())
            .registerTypeAdapter(Date.class, new DateJsonAdapter())
            .registerTypeAdapter(LocalDate.class, new LocalDateJsonAdapter())
            .create();
    private final ImagesReader imagesReader;
    private final ImageManagerJDBCUtills imageManagerDBUtills;

    private final ImageDeduplicationManager imageDeduplicationManager;

    private final Integer endOflifeTimeout;

    static final String MISSING_IMAGE_DATA_IMG = "/META-INF/missingImage.png";
    static final String DELETED_IMAGE_DATA_IMG = "/META-INF/deletedImage.png";


    byte[] missingImageData;
    byte[] deletedImageData;


    public ImageManagerHanlderUtilities(DatabaseManager imageDatabaseManager, ImageRegistry imageRegistry, ImagesReader imagesReader, ImageManagerJDBCUtills imageManagerDBUtills, ImageDeduplicationManager imageDeduplicationManager, Integer endOflifeTimeout) {
        this.imageDatabaseManager = imageDatabaseManager;
        this.imageRegistry = imageRegistry;
        this.imagesReader = imagesReader;
        this.imageManagerDBUtills = imageManagerDBUtills;
        this.imageDeduplicationManager = imageDeduplicationManager;
        this.endOflifeTimeout = endOflifeTimeout;


        try {
            try (InputStream inputStream = this.getClass().getResourceAsStream(MISSING_IMAGE_DATA_IMG)) {
                missingImageData = ByteStreams
                        .toByteArray(inputStream);
            }
        } catch (IOException e) {
            log.error("FAILED to initialize missingImageData", e);
        }

        try {
            try (InputStream inputStream = this.getClass().getResourceAsStream(DELETED_IMAGE_DATA_IMG)) {
                deletedImageData = ByteStreams
                        .toByteArray(inputStream);
            }
        } catch (IOException e) {
            log.error("FAILED to initialize missingImageData", e);
        }
    }

    public long getIdByUUID(String uuid) throws ImageNotFoundException {
        Long id = imageRegistry.getImageIdForKey(UUID.fromString(uuid));
        if (id == null) {
            throw new ImageNotFoundException("no ID found");
        }
        return id;
    }

    public <T> T readInputJson(final HttpServerExchange exchange, Class<T> clazz) throws ValuePhoneException {
        Object[] retVal = new Object[1];
        try {
            exchange.getRequestReceiver().receiveFullString((exchange1, message) -> retVal[0] = gson.fromJson(message, clazz));
        } catch (com.google.gson.JsonSyntaxException e) {
            log.warn(e.getMessage(), e);
            throw new ValuePhoneException("invalid input");
        }
        return clazz.cast(retVal[0]);
    }

    public ImageEntity directCopy(ImageEntity imageToClone) throws ValuePhoneException {
        ImageEntity res;

        ImageType imageType = imageToClone.getImageType();
        res = registerImageForImageType(
                imageToClone.getName(), imageToClone.getMimeType(),
                imageType);
        res.setModificationDate(imageToClone.getModificationDate());

        res.setup(imageToClone);
        imageManagerDBUtills.updateImageEntity(res);

        return res;
    }


    public ImageEntity registerImageForImageType(String fileName, String mimeType, ImageType imageType) throws ValuePhoneException {
        Reject.ifNull(fileName, "no file name");
        imageType = imageType == null ? ImageType.UNKNOWN : imageType;

        ImageEntity image = new ImageEntity();
        image.setName(fileName);
        image.setMimeType(mimeType == null ? "image/unknown" : mimeType);
        image.setImageType(imageType);
        imageManagerDBUtills.saveImageEntity(image);

        return image;
    }

    private ImageLifetime createAndReturnExtendedImageLifetimeForValidFromAndValidTillAndImageLifetime(
            OffsetDateTime validFrom, OffsetDateTime validTill, ImageLifetime currentImageLifetime) {
        OffsetDateTime extendedValidFrom = isInvalidLifetime(currentImageLifetime)
                || currentImageLifetime.validFrom == null
                || currentImageLifetime.validFrom.isAfter(validFrom) ?
                validFrom : currentImageLifetime.validFrom;
        OffsetDateTime extendedValidTill = isInvalidLifetime(currentImageLifetime)
                || currentImageLifetime.validTill == null
                || currentImageLifetime.validTill.isBefore(validTill) ?
                validTill : currentImageLifetime.validTill;
        return new ImageLifetime(
                extendedValidFrom, extendedValidTill);
    }

    public long publishImageWithLifetimeAndReturnDeduplicatedId(long imageId, OffsetDateTime validFrom, OffsetDateTime validTill) throws ValuePhoneException {
        Reject.ifNull(validFrom, "no valid from");
        Reject.ifNull(validTill, "no valid till");
        Reject.ifTrue(validFrom.isAfter(validTill), "valid from is after valid till");
        Reject.ifTrue(validTill.isBefore(OffsetDateTime.now()), "valid till is before now");

        ImageEntity imageEntity = imageManagerDBUtills.getImageById(imageId);

        ImageLifetimeKey lifetimeKey = ImageDeduplicationManager
                .createAndReturnLifetimeKeyForDeduplicatedImageIdAndType(
                        imageEntity.getDeduplicatedImageId());
        ImageLifetime currentImageLifetime =
                imageDeduplicationManager.getLifetimeForImageLifetimeKey(lifetimeKey);

        imageEntity.setValidFrom(validFrom);
        imageEntity.setValidTill(validTill);
        imageEntity.setEndOfLife(validTill.plusDays(endOflifeTimeout).toLocalDate());

        if (isInvalidLifetime(currentImageLifetime)) {
            ImageLifetime initialImageLifetime = new ImageLifetime(validFrom, validTill);

            publishImageToAppServerAndCacheItsLifetime(initialImageLifetime, imageEntity, lifetimeKey);
        } else {
            ImageLifetime modifiedImageLifetime =
                    createAndReturnExtendedImageLifetimeForValidFromAndValidTillAndImageLifetime(
                            validFrom, validTill, currentImageLifetime);

            if (!currentImageLifetime.equals(modifiedImageLifetime) &&
                    !modifyLifetimeForImageOnAppServerAndCacheAndReturnTrueIfSuccessful(
                            currentImageLifetime, modifiedImageLifetime, lifetimeKey))
                return publishImageWithLifetimeAndReturnDeduplicatedId(imageId, validFrom, validTill);
        }

        imageManagerDBUtills.updateImageEntity(imageEntity);
        return imageEntity.getDeduplicatedImageId();
    }

    private void publishImageToAppServerAndCacheItsLifetime(ImageLifetime initialImageLifetime, ImageEntity imageEntity,
                                                            ImageLifetimeKey lifetimeKey) throws ValuePhoneException {
        byte[] content = imageManagerDBUtills.getImageBytes(imageEntity.getDeduplicatedImageId());
        if (content != null) {
            Dimension dimension = getHeightAndWidthOfTheImage(content);

            if (imageDeduplicationManager.cacheLifetimeForImageLifetimeKeyIfAbsent(
                    initialImageLifetime, lifetimeKey) == null) {
                doFinallyPublishImageAndReturnSuccess(imageEntity.getMimeType(), content,
                        imageEntity.getDeduplicatedImageId(),
                        imageEntity.getImageType(), dimension.width, dimension.height,
                        initialImageLifetime.validFrom == null ? null : initialImageLifetime.validFrom.toLocalDateTime(), initialImageLifetime.validTill == null ? null : initialImageLifetime.validTill.toLocalDateTime());
            }
        } else {
            String msg = "Cannot publish image. Missing data for " + imageEntity.getDeduplicatedImageId();
            log.error(msg);
            throw new ValuePhoneException(msg);
        }
    }

    private boolean doFinallyPublishImageAndReturnSuccess(String mimeType, byte[] content, long id, ImageType imageType,
                                                          int width, int height, LocalDateTime validFrom, LocalDateTime validTill) throws ValuePhoneException {
        try {

            imageType = imageType == null ? ImageType.UNKNOWN : imageType;

            log.debug("doFinallyPublishImageAndReturnSuccess(" + id + ", " + imageType.name() + ") -> " +
                    "imageUpsert(mime[" + mimeType + "],  contentBytes[" + content.length + "], id[" + id + "], " +
                    "imagePoolType[" + imageType.getImagePoolType().name() + "], " +
                    "height[" + height + "], width[" + width + "], " +
                    "from[" + validFrom + "], till[" + validTill + "])");

            final ImageMetadata metadata = new ImageMetadata(id, imageType.getImagePoolType().name(), ImageMimeType.fromString(mimeType), width, height, validFrom, validTill);
            ImageUploadHandler.imageUpsert(imageDatabaseManager, content, metadata);


            return true;
        } catch (ImageException e) {
            log.error(e.getMessage(), e);
            throw new ValuePhoneException(e, "FAILED to doFinallyPublishImageAndReturnSuccess()");
        }
    }

    public boolean isInvalidLifetime(ImageLifetime currentImageLifetime) {
        return currentImageLifetime.isMissingLifetime
                || currentImageLifetime.isOutdatedLifetime();
    }

    public Dimension getHeightAndWidthOfTheImage(byte[] image) throws ValuePhoneException {
        Dimension res = new Dimension(0, 0);
        if (image != null) {
            try {
                ImageInfo sii = SimpleImageInfoUtilities.getImageInfo(image);
                res.height = sii.getHeight();
                res.width = sii.getWidth();
            } catch (Exception ex) {
                log.warn(
                        "Problems to analyze image dimension with SimpleImageInfo, start to try it with the old behaviour",
                        ex);
                try {
                    ByteArrayInputStream bais = new ByteArrayInputStream(image);
                    BufferedImage bim = ImageIO.read(bais);
                    res.height = bim.getHeight();
                    res.width = bim.getWidth();
                } catch (Exception e2) {
                    log.error(e2.getMessage(), e2);
                    throw new ValuePhoneException(e2);
                }
            }
        }
        return res;
    }

    public ImageEntity deduplicateImageForDataAndReturnDeduplicatedEntity(ImageEntity imageEntity, byte[] imageData)
            throws ValuePhoneException {

        ImageDeduplicationKey deduplicationKey = ImageDeduplicationManager
                .createAndReturnDeduplicationKeyForImageNameAndImageData(
                        imageEntity.getName(), imageData, imageEntity.getImageType());

        long deduplicatedImageId = imageDeduplicationManager.getIdForImageDeduplicationKey(
                deduplicationKey);

        if (deduplicatedImageId == 0 || deduplicatedImageId == -1) {
            imageEntity.setDeduplicatedImageId(imageEntity.getId());
        } else {
            imageEntity.setDeduplicatedImageId(deduplicatedImageId);
        }

        imageManagerDBUtills.updateImageEntity(imageEntity);
        return imageEntity;
    }

    boolean modifyLifetimeForImageOnAppServerAndCacheAndReturnTrueIfSuccessful(
            ImageLifetime expectedImageLifetime, ImageLifetime modifiedImageLifetime,
            ImageLifetimeKey lifetimeKey) throws ValuePhoneException {
        if (imageDeduplicationManager.replaceLifetimeForLifetimeKey(
                expectedImageLifetime, modifiedImageLifetime, lifetimeKey)) {

            return doFinallyModifyImageLifetimeAndReturnSuccess(
                    lifetimeKey.deduplicatedImageId,
                    modifiedImageLifetime.validFrom == null ? null : modifiedImageLifetime.validFrom.toLocalDateTime(),
                    modifiedImageLifetime.validTill == null ? null : modifiedImageLifetime.validTill.toLocalDateTime());
        } else {
            log.warn("Concurrent modification on imageLifetimeKey[" + lifetimeKey.toString() + "] detected " +
                    "-> retry required ");

            return false;
        }
    }

    private boolean doFinallyModifyImageLifetimeAndReturnSuccess(long id, LocalDateTime validFrom,
                                                                 LocalDateTime validTill)
            throws ValuePhoneException {
        try {


            log.debug("doFinallyModifyImageLifetime(" + id + ", " + validFrom + ", "
                    + validTill + ") -> " +
                    "updateLifetimeForImageAndPoolType(id[" + id + "], " +
                    "from[" + validFrom + "], till[" + validTill + "])");
            try {

                ImageModifyHandler.modifyImage(imageDatabaseManager, id, validFrom, validTill);

            } catch (ImageNotFoundException e) {
                republish(id, validFrom, validTill);
            }
            return true;
        } catch (ImageException e) {
            log.error(e.getMessage(), e);
            throw new ValuePhoneException(e,
                    "FAILED to modifyLifetimeForImageOnAppServerAndCacheAndReturnTrueIfSuccessful");
        }
    }

    private void republish(long id, LocalDateTime validFrom,
                           LocalDateTime validTill) throws ValuePhoneException, ImageException {

        ImageEntity entity = imageManagerDBUtills.getImageById(id);
        byte[] content = imageManagerDBUtills.getImageBytes(entity.getDeduplicatedImageId());
        if(content == null){
            throw new ImageException("image with id {} has no content", id);
        }
        Dimension dimension = getHeightAndWidthOfTheImage(content);

        String poolTypeName = Optional.ofNullable(entity).map(ImageEntity::getImageType).orElse(ImageType.UNKNOWN).getImagePoolType().name();

        final ImageMetadata metadata = new ImageMetadata(id, poolTypeName, ImageMimeType.fromString(entity.getMimeType()), dimension.width, dimension.height, validFrom, validTill);
        ImageUploadHandler.imageUpsert(imageDatabaseManager, content, metadata);

    }

    public void sendLongAsResponse(final HttpServerExchange exchange, Long number) {
        exchange.setStatusCode(StatusCodes.OK);
        final HeaderMap responseHeaders = exchange.getResponseHeaders();
        responseHeaders.put(Headers.CONTENT_TYPE, "application/json");
        exchange.getResponseSender().send(number.toString());
        exchange.endExchange();
    }

    public <T> void sendObjectAsJsonResponse(final HttpServerExchange exchange, T object) {
        sendObjectAsJsonResponse(exchange, object, true);
    }

    public <T> void sendObjectAsJsonResponse(final HttpServerExchange exchange, T object, boolean closeExchange) {
        exchange.setStatusCode(StatusCodes.OK);
        final HeaderMap responseHeaders = exchange.getResponseHeaders();
        responseHeaders.put(Headers.CONTENT_TYPE, "application/json");
        exchange.getResponseSender().send(gson.toJson(object));
        if (closeExchange) {
            exchange.endExchange();
        }
    }


    public String getImageTypeFromMimeType(String resultMimeType) {
        String[] mimeSplit = resultMimeType.split("/");
        return mimeSplit[1].toLowerCase();
    }

    public void validateImageURL(String imageUrl) throws MalformedURLException {
        if (!(imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
            throw new MalformedURLException("Invalid URL string, URL has to start with : http://");
        }
    }

    void checkPreConditionsForScaleImage(Object imageObject, String resultMimeType) throws ValuePhoneException {
        if (imageObject == null)
            throw new ValuePhoneException("image data must not be null");

        if (!checkMimeType(resultMimeType))
            throw new ValuePhoneException("unsupported mimeType[" + resultMimeType + "]");
    }

    private boolean checkMimeType(String resultMimeType) {
        for (String mimeType : Arrays.asList("image/png", "image/jpeg", "image/jpg", "image/webp")) {
            if (resultMimeType.startsWith(mimeType)) {
                return true;
            }
        }

        return false;
    }

    public byte[] scaleImage(byte[] image, String resultMimeType, int maxWidth, int maxHeight, float quality)
            throws ValuePhoneException {
        if (image == null) {
            throw new ValuePhoneException("image data must not be null");
        }

        ImageInfo sii = null;
        try {
            sii = SimpleImageInfoUtilities.getImageInfo(image);
        } catch (IOException e) {
            log.warn(
                    "Problems to analyze image dimension with SimpleImageInfo, start to try it with the old behaviour",
                    e);
        }
        if (resultMimeType == null && sii != null) {
            resultMimeType = sii.getMimeType();
        }

        checkPreConditionsForScaleImage(image, resultMimeType);

        try {
            if (sii == null || sii.getWidth() > maxWidth || sii.getHeight() > maxHeight) {
                if (sii == null)
                    log.warn("image size unknown -> scale it down");
                else
                    log.info("max imagesize exceeded (" + sii.getWidth() + "," + sii.getHeight()
                            + "), scale it down");

                return scaleImage(imagesReader.readImage(image), resultMimeType, maxWidth, maxHeight, quality);
            } else {
                log.info("imagesize (" + sii.getWidth() + "," + sii.getHeight() + ") ok, keep bytes untouched");
                return image;
            }
        } catch (IOException | ImageReadException e) {
            log.error(e.getMessage(), e);
            throw new ValuePhoneException(e);
        }
    }

    public byte[] scaleImage(BufferedImage bufferedImage, String resultMimeType, int maxWidth, int maxHeight,
                             float quality) throws ValuePhoneException, IOException {
        checkPreConditionsForScaleImage(bufferedImage, resultMimeType);
        String imageType = getImageTypeFromMimeType(resultMimeType);

        BufferedImage scaledImage;
        if (bufferedImage.getWidth() > maxWidth || bufferedImage.getHeight() > maxHeight) {
            float widthMultiplier = (float) bufferedImage.getWidth() / (float) maxWidth;
            float heightMultiplier = (float) bufferedImage.getHeight() / (float) maxHeight;

            int width = maxWidth;
            int height = maxHeight;

            if (widthMultiplier < heightMultiplier) {
                width = (int) (bufferedImage.getWidth() * (1 / heightMultiplier));
            } else {
                height = (int) (bufferedImage.getHeight() * (1 / widthMultiplier));
            }

            scaledImage = new BufferedImage(width, height, bufferedImage.getType());
            Graphics2D graphics2D = scaledImage.createGraphics();
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics2D.drawImage(bufferedImage, 0, 0, width, height, null);

            // clean up
            graphics2D.dispose();
        } else {
            scaledImage = bufferedImage;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        if (imageType.equalsIgnoreCase("jpeg")) {
            Iterator<?> iterator = ImageIO.getImageWritersByFormatName(imageType);
            ImageWriter writer = (ImageWriter) iterator.next();
            ImageWriteParam iwp = writer.getDefaultWriteParam();
            iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            iwp.setCompressionQuality(quality);
        }

        try {
            ImageIO.write(scaledImage, imageType, byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new ValuePhoneException(e);
        }
    }

    public byte[] getImageBytes(long imageId) {
        return imageManagerDBUtills.getImageBytes(imageId);
    }

    public byte[] getImageBytes(long imageId, String mimeType, Integer width, Integer height, Float quality)
            throws IOException, ValuePhoneException {
        byte[] imageData = imageId == -1 ? deletedImageData : imageManagerDBUtills.getImageBytes(imageId);

        if (imageData == null) {
            ImageEntity imageEntity = imageManagerDBUtills.getImageById(imageId);
            imageData = imageEntity.isDeleted() ? deletedImageData : missingImageData;
        }

        if (width == null && height == null && quality == null)
            return imageData;

        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageData));
        if (width == null || height == null) {
            float ratio = ((float) bufferedImage.getWidth()) / ((float) bufferedImage.getHeight());
            if (width != null) {
                height = (int) (width * ratio);
            }
            if (width == null && height != null) {
                width = (int) (ratio * height);
            }
            if (width == null) {
                width = bufferedImage.getWidth();
                height = bufferedImage.getHeight();
            }
        }
        if (quality == null)
            quality = 0.9f;
        return scaleImage(bufferedImage, mimeType, width, height, quality);
    }


}
