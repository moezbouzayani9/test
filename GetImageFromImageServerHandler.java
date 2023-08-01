package com.valuephone.image.management.images.handler;

import com.valuephone.image.dto.ImageEntity;
import com.valuephone.image.dto.OutputImageMetadata;
import com.valuephone.image.dto.OutputUserImage;
import com.valuephone.image.exception.ImageException;
import com.valuephone.image.exception.ImageNotFoundException;
import com.valuephone.image.exception.ValuePhoneException;
import com.valuephone.image.handler.CachingImageAccessHandler;
import com.valuephone.image.handler.HybridAuthenticatedWrapperHandler;
import com.valuephone.image.handler.UserImageAccessHandler;
import com.valuephone.image.helper.*;
import com.valuephone.image.management.images.jdbc.ImageManagerJDBCUtills;
import com.valuephone.image.management.share.images.ImagePoolType;
import com.valuephone.image.management.share.images.ImageType;
import com.valuephone.image.security.HybridImageAccount;
import com.valuephone.image.security.ImageUserPrincipal;
import com.valuephone.image.utilities.CheckUtilities;
import com.valuephone.image.utilities.Constants;
import com.valuephone.image.utilities.DatabaseManager;
import com.valuephone.image.utilities.HandlerUtilities;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Principal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;


@Slf4j
public class GetImageFromImageServerHandler extends HybridAuthenticatedWrapperHandler {

    private final ImageManagerHanlderUtilities imageManagerHanlderUtilities;
    private final ImageManagerJDBCUtills imageManagerDBUtills;
    private final CachingImageAccessHandler cachingImageAccessHandler;
    private final DatabaseManager imageDatabaseManager;

    public GetImageFromImageServerHandler(ImageManagerHanlderUtilities imageManagerHanlderUtilities, ImageManagerJDBCUtills imageManagerDBUtills, CachingImageAccessHandler cachingImageAccessHandler, DatabaseManager imageDatabaseManager) {
        this.imageManagerHanlderUtilities = imageManagerHanlderUtilities;
        this.imageManagerDBUtills = imageManagerDBUtills;
        this.cachingImageAccessHandler = cachingImageAccessHandler;
        this.imageDatabaseManager = imageDatabaseManager;
    }

    @Override
    protected void handleRequestAuthenticated(HttpServerExchange exchange, Principal principal, HybridImageAccount account) throws Exception {
        final Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        final Long imageId = HandlerUtilities.readLongParam(queryParameters, Constants.IMAGE_ID);
        final ImageType imageType = java.util.Optional.ofNullable(HandlerUtilities.readStringParam(queryParameters, Constants.IMAGE_TYPE)).map(ImageType::valueOf).orElse(null);
        final Integer width = HandlerUtilities.readIntegerParam(queryParameters, Constants.WIDTH);
        final Integer height = HandlerUtilities.readIntegerParam(queryParameters, Constants.HEIGHT);

        CheckUtilities.checkArgumentNotNull(imageId, Constants.IMAGE_ID);
        CheckUtilities.checkArgumentNotNull(width, Constants.WIDTH);
        CheckUtilities.checkArgumentNotNull(height, Constants.HEIGHT);

        ImageEntity retVal = getImageFromImageServer(imageId, imageType, width, height, principal, account);
        imageManagerHanlderUtilities.sendObjectAsJsonResponse(exchange, retVal);
    }

    public ImageEntity getImageFromImageServer(long imageId, ImageType imageType, int width, int height, Principal principal, HybridImageAccount account)
            throws ValuePhoneException {

        byte[] binary;
        String mimeType;

        imageType = imageType == null ? ImageType.UNKNOWN : imageType;

        if (ImagePoolType.USER_IMAGE.equals(imageType.getImagePoolType()) ||
                ImagePoolType.USER_AVATAR.equals(imageType.getImagePoolType())) {

            if(!account.isUser()){
                log.warn("wrong user type! (should be user), not authorised to perform this operation");
                throw new SecurityException("not authorised");
            }

            try (Connection connection = imageDatabaseManager.getConnection()) {

                if(! (principal instanceof ImageUserPrincipal)){
                    log.error("wrong user principals");
                    throw new SecurityException("wrong user principals");
                }

                final OutputUserImage userImage = UserImageAccessHandler.getUserImage(imageId, (ImageUserPrincipal) principal, connection);


                final CustomImageReader customImageReader = new CustomImageReader(userImage.getImageBytes());
                customImageReader.resizeTo(width, height);
                final CustomImageWriter writer = customImageReader.toWriter();
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    writer.writeImage(outputStream);
                    binary = outputStream.toByteArray();
                } catch (IOException e) {
                    log.error("can't resize image", e);
                    throw new ValuePhoneException("can't resize image");
                }

                mimeType = userImage.getMimeType().toString();

            } catch (SQLException e) {
                log.error("SQL error", e);
                throw new ValuePhoneException("SQL error");
            }
        } else {

            if(!account.isStaticUser()){
                log.warn("wrong user type! (should be static), not authorised to perform this operation");
                throw new SecurityException("not authorised");
            }

            try {
                OutputImageMetadata imageMetadata;

                try (final Connection connection = imageDatabaseManager.getConnection()) {
                    imageMetadata = ImageServiceManager.getImageMetadata(imageId, connection);
                    cachingImageAccessHandler.checkImageValidity(imageMetadata);
                    final ImageSize size = new ImageSize(imageMetadata.getWidth(), imageMetadata.getHeight());

                    if (ImageManipulationManager.resize(size, width, height)) {
                        binary = cachingImageAccessHandler.getImageBytesFromFileCache(size, imageMetadata);
                    } else {
                        log.debug("No resize needed - returning raw image");
                        binary = cachingImageAccessHandler.getImageBytesFromCache(imageMetadata.getBinaryDataId());
                    }
                    mimeType = imageMetadata.getMimeType().toString();
                } catch (ExecutionException | SQLException e) {
                    log.error(e.getMessage(), e);
                    throw new ValuePhoneException("can't  get image from database");
                }
            }catch (ImageNotFoundException e){
                log.warn("image not found");
                throw e;
            } catch (ImageException e) {
                log.warn("FAILED to getImageFromImageServer()", e);
                throw new ValuePhoneException(e, e.getMessage());
            }
        }

        ImageEntity imageEntity = imageManagerHanlderUtilities.registerImageForImageType(Objects.toString(imageType) + "_" + imageId, mimeType, imageType
        );
        imageEntity = imageManagerHanlderUtilities.deduplicateImageForDataAndReturnDeduplicatedEntity(imageEntity, binary);
        imageEntity.setModificationDate(OffsetDateTime.now());
        if (Objects.equals(imageEntity.getId(), imageEntity.getDeduplicatedImageId())) {
            imageManagerDBUtills.persistImageData(imageEntity, binary);
        }
        return imageEntity;

    }
}
