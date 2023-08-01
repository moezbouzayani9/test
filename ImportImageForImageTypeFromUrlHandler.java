package com.valuephone.image.management.images.handler;

import com.valuephone.image.dto.ImageEntity;
import com.valuephone.image.exception.ImportWrongImageException;
import com.valuephone.image.exception.ValuePhoneException;
import com.valuephone.image.management.images.*;
import com.valuephone.image.management.images.jdbc.ImageManagerJDBCUtills;
import com.valuephone.image.management.share.images.ImageConfiguration;
import com.valuephone.image.management.share.images.ImageType;
import com.valuephone.image.utilities.CheckUtilities;
import com.valuephone.image.utilities.Constants;
import com.valuephone.image.utilities.HandlerUtilities;
import com.valuephone.image.utilities.SimpleImageInfoUtilities;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.FileNameMap;
import java.net.URL;
import java.net.URLConnection;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;


@Slf4j
public class ImportImageForImageTypeFromUrlHandler implements HttpHandler {

    private final ImageManagerHanlderUtilities imageManagerHanlderUtilities;
    private final ImageDownloadHelper imageDownloadHelper;
    private final ImageConfigurationManager imageConfigurationManager;
    private final ImageValidator imageValidator;
    private final ImageManagerJDBCUtills imageManagerDBUtills;

    public ImportImageForImageTypeFromUrlHandler(ImageManagerHanlderUtilities imageManagerHanlderUtilities, ImageDownloadHelper imageDownloadHelper, ImageConfigurationManager imageConfigurationManager, ImageValidator imageValidator, ImageManagerJDBCUtills imageManagerDBUtills) {
        this.imageManagerHanlderUtilities = imageManagerHanlderUtilities;
        this.imageDownloadHelper = imageDownloadHelper;
        this.imageConfigurationManager = imageConfigurationManager;
        this.imageValidator = imageValidator;
        this.imageManagerDBUtills = imageManagerDBUtills;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        final Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        final ImageType imageType = HandlerUtilities.readEnumParam(queryParameters, Constants.IMAGE_TYPE, ImageType.class);
        final String imageUrl = HandlerUtilities.readStringParam(queryParameters, Constants.IMAGE_URL);

        CheckUtilities.checkArgumentNotNull(imageType, Constants.IMAGE_TYPE);
        CheckUtilities.checkArgumentNotNull(imageUrl, Constants.IMAGE_URL);

        ImageEntity imageEntity = importImageForBoImageTypeFromUrl(imageType, imageUrl);

        imageManagerHanlderUtilities.sendObjectAsJsonResponse(exchange, imageEntity);
    }

    public ImageEntity importImageForBoImageTypeFromUrl(ImageType imageType, String imageUrl)
            throws IOException, ValuePhoneException, ImportWrongImageException {
        log.debug("try to load " + imageUrl);

        ImageEntity imageEntity;
        String mimeType;

        imageManagerHanlderUtilities.validateImageURL(imageUrl);

        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        mimeType = fileNameMap.getContentTypeFor(imageUrl);

        URL url = new URL(imageUrl);

        ImageDownloadResult imageDownloadResult = imageDownloadHelper.downloadImageFromUrl(url);

        try {
            ImageInfo sii = SimpleImageInfoUtilities.getImageInfo(imageDownloadResult.getImageData());
            imageEntity = imageManagerHanlderUtilities.registerImageForImageType(imageUrl, (mimeType == null) ?
                    sii.getMimeType() : mimeType, imageType);
        } catch (IOException e) {
            log.warn(e.getMessage(), e);
            throw new ImportWrongImageException(e.getMessage());
        }

        imageEntity.setModificationDate(imageDownloadResult.getModificationDate());

        Long imageId = imageEntity.getId();

        ImageConfiguration imageConfiguration = imageConfigurationManager.getImageConfiguration(imageType);
        byte[] scaledImage = imageManagerHanlderUtilities.scaleImage(imageDownloadResult.getImageData(), mimeType,
                imageConfiguration.getMaxWidth(), imageConfiguration.getMaxHeight(), 0.95f);

        imageValidator.validateImageFileSize(scaledImage, imageType);
        imageEntity = imageManagerHanlderUtilities.deduplicateImageForDataAndReturnDeduplicatedEntity(imageEntity, scaledImage);
        if (Objects.equals(imageId, imageEntity.getDeduplicatedImageId()))
            imageManagerDBUtills.persistImageData(imageEntity, scaledImage);

        return imageEntity;
    }


}
