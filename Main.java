package com.valuephone.image;

import com.valuephone.image.cache.ImageDeduplicationCache;
import com.valuephone.image.cache.ImageDeduplicationCacheBean;
import com.valuephone.image.exception.*;
import com.valuephone.image.handler.*;
import com.valuephone.image.management.images.*;
import com.valuephone.image.management.images.handler.*;
import com.valuephone.image.management.images.jdbc.ImageManagerJDBCUtills;
import com.valuephone.image.management.share.images.ImagesReader;
import com.valuephone.image.management.share.images.ImagesReaderImpl;
import com.valuephone.image.security.DBIdentityManager;
import com.valuephone.image.security.HybridIdentityManager;
import com.valuephone.image.security.SingleIdentityManager;
import com.valuephone.image.task.CacheCleanupTask;
import com.valuephone.image.task.ImageCleanupTask;
import com.valuephone.image.utilities.*;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.attribute.QueryParameterAttribute;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.security.handlers.AuthenticationMechanismsHandler;
import io.undertow.security.handlers.SecurityInitialHandler;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.impl.BasicAuthenticationMechanism;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.ExceptionHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;
import io.undertow.server.handlers.PathHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.SecurityException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static io.undertow.predicate.Predicates.*;

/**
 * @author tcigler
 * @since 1.0
 */
@Slf4j
public class Main {

    // === Constants ===

    private static final String FORCE_STRICT_DATA_SOURCE_AVAILABILITY_CHECK_PROPERTY_NAME = "MCA_FORCE_STRICT_DATA_SOURCE_AVAILABILITY_CHECK";

    private static final String MAIN_DATA_SOURCE_HOST_NAME_ENVIRONMENT_PROPERTY_NAME = "MCA_AS_MAIN_DATA_SOURCE_HOST_NAME";

    private static final String MAIN_DATA_SOURCE_HOST_PORT_ENVIRONMENT_PROPERTY_NAME = "MCA_AS_MAIN_DATA_SOURCE_HOST_PORT";

    private static final String MAIN_DATA_SOURCE_NAME_ENVIRONMENT_PROPERTY_NAME = "MCA_AS_MAIN_DATA_SOURCE_NAME";

    private static final String MAIN_DATA_SOURCE_USER_NAME_ENVIRONMENT_PROPERTY_NAME = "MCA_AS_MAIN_DATA_SOURCE_USER_NAME";

    private static final String MAIN_DATA_SOURCE_SSL_REQUIRED_ENVIRONMENT_PROPERTY_NAME = "MCA_AS_MAIN_DATA_SOURCE_SSL_REQUIRED";

    private static final String MAIN_DATA_SOURCE_USER_PASSWORD_ENVIRONMENT_PROPERTY_NAME = "MCA_AS_MAIN_DATA_SOURCE_USER_PASSWORD";

    private static final String MAIN_DATA_SOURCE_CONNECTION_TIMEOUT_PROPERTY_NAME = "MCA_AS_MAIN_DATA_SOURCE_CONNECTION_TIMEOUT";

    private static final String MAIN_DATA_SOURCE_CONNECTION_VALIDATION_TIMEOUT_PROPERTY_NAME = "MCA_AS_MAIN_DATA_SOURCE_CONNECTION_VALIDATION_TIMEOUT";

    private static final String MAIN_DATA_SOURCE_MIN_POOL_SIZE = "MCA_AS_MAIN_DATA_SOURCE_MIN_POOL_SIZE";

    private static final String MAIN_DATA_SOURCE_MAX_POOL_SIZE = "MCA_AS_MAIN_DATA_SOURCE_MAX_POOL_SIZE";

    private static final String MAIN_DATA_SOURCE_POOL_NAME = "MainDatabaseAuthenticatorDataSource";

    private static final String IMAGE_DATA_SOURCE_HOST_NAME_ENVIRONMENT_PROPERTY_NAME = "MCA_IMAGE_DATA_SOURCE_HOST_NAME";

    private static final String IMAGE_DATA_SOURCE_HOST_PORT_ENVIRONMENT_PROPERTY_NAME = "MCA_IMAGE_DATA_SOURCE_HOST_PORT";

    private static final String IMAGE_DATA_SOURCE_NAME_ENVIRONMENT_PROPERTY_NAME = "MCA_IMAGE_DATA_SOURCE_NAME";

    private static final String IMAGE_DATA_SOURCE_USER_NAME_ENVIRONMENT_PROPERTY_NAME = "MCA_IMAGE_DATA_SOURCE_USER_NAME";

    private static final String IMAGE_DATA_SOURCE_SSL_REQUIRED_ENVIRONMENT_PROPERTY_NAME = "MCA_IMAGE_DATA_SOURCE_SSL_REQUIRED";

    private static final String IMAGE_DATA_SOURCE_USER_PASSWORD_ENVIRONMENT_PROPERTY_NAME = "MCA_IMAGE_DATA_SOURCE_USER_PASSWORD";

    private static final String IMAGE_DATA_SOURCE_CONNECTION_TIMEOUT_PROPERTY_NAME = "MCA_IMAGE_DATA_SOURCE_CONNECTION_TIMEOUT";

    private static final String IMAGE_DATA_SOURCE_CONNECTION_VALIDATION_TIMEOUT_PROPERTY_NAME = "MCA_IMAGE_DATA_SOURCE_CONNECTION_VALIDATION_TIMEOUT";

    private static final String IMAGE_DATA_SOURCE_MIN_POOL_SIZE = "MCA_IMAGE_DATA_SOURCE_MIN_POOL_SIZE";

    private static final String IMAGE_DATA_SOURCE_MAX_POOL_SIZE = "MCA_IMAGE_DATA_SOURCE_MAX_POOL_SIZE";

    private static final String IMAGE_DATA_SOURCE_POOL_NAME = "ImageDatabaseWriterDataSource";

    private static final String MCA_MH_DATA_SOURCE_HOST_NAME_ENVIRONMENT_PROPERTY_NAME = "MCA_MH_DATA_SOURCE_HOST_NAME";
    private static final String MCA_MH_DATA_SOURCE_HOST_PORT_ENVIRONMENT_PROPERTY_NAME = "MCA_MH_DATA_SOURCE_HOST_PORT";
    private static final String MCA_MH_DATA_SOURCE_NAME_ENVIRONMENT_PROPERTY_NAME = "MCA_MH_DATA_SOURCE_NAME";
    private static final String MCA_MH_DATA_SOURCE_USER_NAME_ENVIRONMENT_PROPERTY_NAME = "MCA_MH_DATA_SOURCE_USER_NAME";
    private static final String MCA_MH_DATA_SOURCE_USER_PASSWORD_ENVIRONMENT_PROPERTY_NAME = "MCA_MH_DATA_SOURCE_USER_PASSWORD";
    private static final String MCA_MH_DATA_SOURCE_SSL_REQUIRED_ENVIRONMENT_PROPERTY_NAME = "MCA_MH_DATA_SOURCE_SSL_REQUIRED";
    private static final String MCA_MH_DATA_SOURCE_CONNECTION_TIMEOUT_PROPERTY_NAME = "MCA_MH_DATA_SOURCE_CONNECTION_TIMEOUT";
    private static final String MCA_MH_DATA_SOURCE_CONNECTION_VALIDATION_TIMEOUT_PROPERTY_NAME = "MCA_MH_DATA_SOURCE_CONNECTION_VALIDATION_TIMEOUT";
    private static final String MCA_MH_DATA_SOURCE_MIN_POOL_SIZE = "MCA_MH_DATA_SOURCE_MIN_POOL_SIZE";
    private static final String MCA_MH_DATA_SOURCE_MAX_POOL_SIZE = "MCA_MH_DATA_SOURCE_MAX_POOL_SIZE";
    private static final String MCA_MH_DATA_SOURCE_POOL_NAME = "MHDatabaseDataSource";

    static final String IMAGE_MANAGER_END_OF_LIFE_TIMEOUT_PROPERTY_NAME = "com.valuephone.management.images.ImageManager.endOfLifeTimeout";
    static final Integer IMAGE_MANAGER_END_OF_LIFE_TIMEOUT_PROPERTY_DEFAULT = 30;

    private static final String ADMIN_USER_NAME = "MCA_IMAGE_ADMIN_USER_NAME";

    private static final String ADMIN_USER_PASSWORD_HASH = "MCA_IMAGE_ADMIN_USER_PASSWORD_HASH";

    private static IdentityManager ADMIN_IDENTITY_MANAGER;

    private static final String ADMINISTRATION_REALM_NAME = "Administration realm";
    private static final String USER_REALM_NAME = "User realm";

    private static final String IMAGE_MAINTENANCE_URL_TEMPLATE = "/protected/maintenance/image/{" + Constants.POOL_ID_PARAMETER + "}/{" + Constants.IMAGE_ID_PARAMETER + "}";
    private static final String IMAGE_METADATA_UPDATE_URL_TEMPLATE = "/protected/update/image/{" + Constants.POOL_ID_PARAMETER + "}/{" + Constants.IMAGE_ID_PARAMETER + "}";
    private static final String USER_IMAGE_MAINTENANCE_URL_TEMPLATE = "/protected/maintenance/userimage";

    private static final int DEFAULT_HTTP_PORT = 8080;
    private static final int DEFAULT_AJP_PORT = 8009;

    public static void main(String[] args) {

        environmentInitialization(args);
    }

    private static void environmentInitialization(String[] args) {

        try {

            final DatabaseManager imageDatabaseManager = new DatabaseManager(
                    getEnvironmentProperty(IMAGE_DATA_SOURCE_HOST_NAME_ENVIRONMENT_PROPERTY_NAME, false),
                    getEnvironmentProperty(IMAGE_DATA_SOURCE_HOST_PORT_ENVIRONMENT_PROPERTY_NAME, false),
                    getEnvironmentProperty(IMAGE_DATA_SOURCE_NAME_ENVIRONMENT_PROPERTY_NAME, false),
                    getEnvironmentProperty(IMAGE_DATA_SOURCE_USER_NAME_ENVIRONMENT_PROPERTY_NAME, false),
                    getEnvironmentProperty(IMAGE_DATA_SOURCE_USER_PASSWORD_ENVIRONMENT_PROPERTY_NAME, true),
                    getOptionalEnvironmentProperty(IMAGE_DATA_SOURCE_SSL_REQUIRED_ENVIRONMENT_PROPERTY_NAME, false).map(p -> p.isEmpty() || "true".equals(p.toLowerCase(Locale.ENGLISH))).orElse(null),
                    getOptionalIntegerEnvironmentProperty(IMAGE_DATA_SOURCE_CONNECTION_TIMEOUT_PROPERTY_NAME, false).filter(v -> v > 0).orElse(null),
                    getOptionalIntegerEnvironmentProperty(IMAGE_DATA_SOURCE_CONNECTION_VALIDATION_TIMEOUT_PROPERTY_NAME, false).filter(v -> v > 0).orElse(null),
                    getOptionalIntegerEnvironmentProperty(IMAGE_DATA_SOURCE_MIN_POOL_SIZE, false).filter(v -> v > 0).orElse(null),
                    getOptionalIntegerEnvironmentProperty(IMAGE_DATA_SOURCE_MAX_POOL_SIZE, false).filter(v -> v > 0).orElse(null),
                    IMAGE_DATA_SOURCE_POOL_NAME
            );

            final DatabaseManager serverDatabaseManager = new DatabaseManager(
                    getEnvironmentProperty(MAIN_DATA_SOURCE_HOST_NAME_ENVIRONMENT_PROPERTY_NAME, false),
                    getEnvironmentProperty(MAIN_DATA_SOURCE_HOST_PORT_ENVIRONMENT_PROPERTY_NAME, false),
                    getEnvironmentProperty(MAIN_DATA_SOURCE_NAME_ENVIRONMENT_PROPERTY_NAME, false),
                    getEnvironmentProperty(MAIN_DATA_SOURCE_USER_NAME_ENVIRONMENT_PROPERTY_NAME, false),
                    getEnvironmentProperty(MAIN_DATA_SOURCE_USER_PASSWORD_ENVIRONMENT_PROPERTY_NAME, true),
                    getOptionalEnvironmentProperty(MAIN_DATA_SOURCE_SSL_REQUIRED_ENVIRONMENT_PROPERTY_NAME, false).map(p -> p.isEmpty() || "true".equals(p.toLowerCase(Locale.ENGLISH))).orElse(null),
                    getOptionalIntegerEnvironmentProperty(MAIN_DATA_SOURCE_CONNECTION_TIMEOUT_PROPERTY_NAME, false).filter(v -> v > 0).orElse(null),
                    getOptionalIntegerEnvironmentProperty(MAIN_DATA_SOURCE_CONNECTION_VALIDATION_TIMEOUT_PROPERTY_NAME, false).filter(v -> v > 0).orElse(null),
                    getOptionalIntegerEnvironmentProperty(MAIN_DATA_SOURCE_MIN_POOL_SIZE, false).filter(v -> v > 0).orElse(null),
                    getOptionalIntegerEnvironmentProperty(MAIN_DATA_SOURCE_MAX_POOL_SIZE, false).filter(v -> v > 0).orElse(null),
                    MAIN_DATA_SOURCE_POOL_NAME
            );

            final DatabaseManager mhDatabaseManager = new DatabaseManager(
                    getEnvironmentProperty(MCA_MH_DATA_SOURCE_HOST_NAME_ENVIRONMENT_PROPERTY_NAME, false),
                    getEnvironmentProperty(MCA_MH_DATA_SOURCE_HOST_PORT_ENVIRONMENT_PROPERTY_NAME, false),
                    getEnvironmentProperty(MCA_MH_DATA_SOURCE_NAME_ENVIRONMENT_PROPERTY_NAME, false),
                    getEnvironmentProperty(MCA_MH_DATA_SOURCE_USER_NAME_ENVIRONMENT_PROPERTY_NAME, false),
                    getEnvironmentProperty(MCA_MH_DATA_SOURCE_USER_PASSWORD_ENVIRONMENT_PROPERTY_NAME, true),
                    getOptionalEnvironmentProperty(MCA_MH_DATA_SOURCE_SSL_REQUIRED_ENVIRONMENT_PROPERTY_NAME, false).map(p -> p.isEmpty() || "true".equals(p.toLowerCase(Locale.ENGLISH))).orElse(null),
                    getOptionalIntegerEnvironmentProperty(MCA_MH_DATA_SOURCE_CONNECTION_TIMEOUT_PROPERTY_NAME, false).filter(v -> v > 0).orElse(null),
                    getOptionalIntegerEnvironmentProperty(MCA_MH_DATA_SOURCE_CONNECTION_VALIDATION_TIMEOUT_PROPERTY_NAME, false).filter(v -> v > 0).orElse(null),
                    getOptionalIntegerEnvironmentProperty(MCA_MH_DATA_SOURCE_MIN_POOL_SIZE, false).filter(v -> v > 0).orElse(null),
                    getOptionalIntegerEnvironmentProperty(MCA_MH_DATA_SOURCE_MAX_POOL_SIZE, false).filter(v -> v > 0).orElse(null),
                    MCA_MH_DATA_SOURCE_POOL_NAME
            );

            Integer endOflifeTimeout = getOptionalIntegerEnvironmentProperty(IMAGE_MANAGER_END_OF_LIFE_TIMEOUT_PROPERTY_NAME, false).orElse(IMAGE_MANAGER_END_OF_LIFE_TIMEOUT_PROPERTY_DEFAULT);


            ADMIN_IDENTITY_MANAGER = new SingleIdentityManager(
                    getEnvironmentProperty(ADMIN_USER_NAME, false),
                    getEnvironmentProperty(ADMIN_USER_PASSWORD_HASH, false)
            );

            boolean strictDatabaseAvailabilityCheckForced = getOptionalEnvironmentProperty(FORCE_STRICT_DATA_SOURCE_AVAILABILITY_CHECK_PROPERTY_NAME, false).map(p -> p.isEmpty() || "true".equals(p.toLowerCase(Locale.ENGLISH))).orElse(false);

            log.debug("Strict database availability check forced: {}", strictDatabaseAvailabilityCheckForced);

            final PathHandler pathHandler = preparePathHandlers(imageDatabaseManager, serverDatabaseManager, mhDatabaseManager, strictDatabaseAvailabilityCheckForced, endOflifeTimeout);

            final ScheduledExecutorService scheduledExecutorService = createAndStartTimerTasks(imageDatabaseManager);

            final GracefulShutdownHandler gracefulShutdownHandler = wrapToShutdownHandlerAndRegisterHook(pathHandler, scheduledExecutorService);

            startUndertowServer(gracefulShutdownHandler, args);

        } catch (Exception e) {
            log.error("Exception during system initialization!", e);
            System.exit(1);
        }
    }

    private static GracefulShutdownHandler wrapToShutdownHandlerAndRegisterHook(final PathHandler pathHandler, final ScheduledExecutorService scheduledExecutorService) {

        final GracefulShutdownHandler gracefulShutdownHandler = new GracefulShutdownHandler(pathHandler);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            gracefulShutdownHandler.shutdown();

            gracefulShutdownHandler.addShutdownListener(shutdownSuccessful -> {

                if (shutdownSuccessful) {
                    scheduledExecutorService.shutdown();
                    try {
                        final int scheduledTasksShutdownTimeout = 5;
                        if (scheduledExecutorService.awaitTermination(scheduledTasksShutdownTimeout, TimeUnit.SECONDS)) {
                            log.info("Scheduled tasks were properly shutdown");
                        } else {
                            log.warn("Scheduled tasks were not shutdown within timeout! Processing will be killed");
                        }
                    } catch (InterruptedException ignore) {
                    }
                }

            });

            try {
                gracefulShutdownHandler.awaitShutdown();
                log.info("Handler shut down");
            } catch (InterruptedException ignore) {
            }
        }
        ));

        return gracefulShutdownHandler;
    }

    private static void startUndertowServer(final HttpHandler pathHandler, final String[] args) {
        final String host = "0.0.0.0";

        int httpPort = getHttpPortToBind(args);
        int ajpPort = getAjpPortToBind(args);

        Undertow server = Undertow.builder()
                .addHttpListener(httpPort, host)
                .addAjpListener(ajpPort, host)
                .setHandler(new HttpLoggingHandler(pathHandler))
                .build();

        server.start();

    }

    /**
     * @param imageDatabaseManager
     * @param serverDatabaseManager
     * @param mhDatabaseManager
     * @param forceStrictDatabaseAvailabilityCheck
     * @param endOflifeTimeout
     * @return
     */
    private static PathHandler preparePathHandlers(final DatabaseManager imageDatabaseManager, final DatabaseManager serverDatabaseManager, DatabaseManager mhDatabaseManager, boolean forceStrictDatabaseAvailabilityCheck, Integer endOflifeTimeout) {

        final IdentityManager userIdentityManager = new DBIdentityManager(serverDatabaseManager);
        final IdentityManager hybridIdentityManager = new HybridIdentityManager(serverDatabaseManager, getEnvironmentProperty(ADMIN_USER_NAME, false),
                getEnvironmentProperty(ADMIN_USER_PASSWORD_HASH, false));

        final QueryParameterAttribute poolIdQueryParameter = new QueryParameterAttribute(Constants.POOL_ID_PARAMETER);

        final ImageRegistry imageRegistry = ImageRegistry.getInstance();

        final ImageManagerJDBCUtills imageManagerDBUtills = new ImageManagerJDBCUtills(mhDatabaseManager);
        final ImageDeduplicationCache imageDeduplicationCache = new ImageDeduplicationCacheBean(mhDatabaseManager);
        final ImageDeduplicationManager imageDeduplicationManager = new ImageDeduplicationManagerBean(imageDeduplicationCache);

        final ImagesReader imagesReader = new ImagesReaderImpl();
        final ImageManagerHanlderUtilities imageManagerHanlderUtilities = new ImageManagerHanlderUtilities(imageDatabaseManager, imageRegistry, imagesReader, imageManagerDBUtills, imageDeduplicationManager, endOflifeTimeout);

        final ImageDownloadHelper imageDownloadHelper = new ImageDownloadHelper();
        final ImageConfigurationManager imageConfigurationManager = new CacheableImageConfigurationManagerBean(mhDatabaseManager);
        final ImageValidator imageValidator = new ImageValidatorImpl(imageConfigurationManager);

        final CachingImageAccessHandler cachingImageAccessHandler = new CachingImageAccessHandler(imageDatabaseManager);

        return Handlers.path().addPrefixPath(
                "/vpimage/rest",
                Handlers.routing()
                        .get("/info", new InformationHTTPHandler(serverDatabaseManager, imageDatabaseManager))
                        .get("/image", and(
                                        exists(new QueryParameterAttribute(Constants.IMAGE_ID_PARAMETER)),
                                        or(
                                                not(exists(poolIdQueryParameter)),
                                                contains(poolIdQueryParameter, Constants.USER_IMAGE_POOL, Constants.USER_AVATAR_POOL)
                                        )
                                ),
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new UserImageAccessHandler(imageDatabaseManager),
                                        USER_REALM_NAME,
                                        userIdentityManager
                                )
                        )
                        .get("/image", and(
                                        exists(new QueryParameterAttribute(Constants.IMAGE_ID_PARAMETER)),
                                        exists(poolIdQueryParameter)
                                ),
                                addExceptionHandlingWithDispatch(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        cachingImageAccessHandler
                                )
                        )
                        .get("/qr",
                                addExceptionHandlingWithDispatch(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new QRCodeHandler()
                                )
                        )
                        .put(IMAGE_METADATA_UPDATE_URL_TEMPLATE,
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new ImageModifyHandler(imageDatabaseManager),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER
                                )
                        )
                        .put(IMAGE_MAINTENANCE_URL_TEMPLATE,
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new ImageUploadHandler(imageDatabaseManager),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER
                                )
                        )
                        .delete(IMAGE_MAINTENANCE_URL_TEMPLATE,
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new ImageDeleteHandler(imageDatabaseManager),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER
                                )
                        )
                        .post(USER_IMAGE_MAINTENANCE_URL_TEMPLATE,
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new UserImageUploadHandler(imageDatabaseManager),
                                        USER_REALM_NAME,
                                        userIdentityManager
                                )
                        )
                        .delete(USER_IMAGE_MAINTENANCE_URL_TEMPLATE,
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new UserImageDeleteHandler(imageDatabaseManager),
                                        USER_REALM_NAME,
                                        userIdentityManager)
                        )
                        .get("protected/user-image-info",
                                exists(new QueryParameterAttribute(Constants.IMAGE_ID_PARAMETER)),
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new UserImageInfoHandler(imageDatabaseManager),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER)
                        )
                        .delete("protected/user-image", // protected/maintenance is already taken by registered user upload and delete
//                                exists(new QueryParameterAttribute(Constants.USER_ID_PARAMETER)),
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new UserImageMaintenanceDeleteHandler(imageDatabaseManager),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER)
                        )
                        // New API migrated from MH
                        .get("images/v1/internal/image",
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new GetImageByIdentifierHandler(imageManagerHanlderUtilities, imageManagerDBUtills),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER)
                        )
                        .delete("images/v1/internal/image",
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new RemoveImageIfPossibleAndReturnResultingEntityHandler(imageManagerHanlderUtilities,imageManagerDBUtills,imageDeduplicationManager),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER)
                        )
                        .get("images/v1/internal/image-id",
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new GetIdByUUIDHandler(imageManagerHanlderUtilities),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER)
                                )
                        .get("images/v1/image",
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new GetRestImageByIdentifierHandler(imageManagerHanlderUtilities, imageManagerDBUtills),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER)
                                )
                        .put("images/v1/image",
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new UploadImageForImageTypeHandler(imageManagerHanlderUtilities, imageManagerDBUtills, imageConfigurationManager, imageRegistry),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER)
                                )
                        .put("images/v1/internal/image/import",
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new ImportImageForImageTypeFromUrlHandler(imageManagerHanlderUtilities, imageDownloadHelper, imageConfigurationManager, imageValidator, imageManagerDBUtills),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER)
                        )
                        .get("images/v1/image/metadata",
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new GetImageMetadataByIdHandler(imageManagerDBUtills, imageRegistry, imageManagerHanlderUtilities),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER)
                                )
                        .put("images/v1/internal/image/publish",
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new PublishImageWithLifetimeAndReturnDeduplicatedIdHandler(imageManagerHanlderUtilities),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER)
                                )
                        .delete("images/v1/internal/image/publish",
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new RevokeImageAndReturnResultingEntityHandler(imageManagerHanlderUtilities,imageManagerDBUtills,endOflifeTimeout,imageDeduplicationManager, imageDatabaseManager),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER)
                                )
                        .put("images/v1/internal/image/copy",
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new DirectCopyHandler(imageManagerHanlderUtilities),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER)
                                )
                        .post("images/v1/internal/image/copy-id",
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new CopyImageByIdAndReturnCopiedImagesIdHandler(imageManagerHanlderUtilities,imageManagerDBUtills),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER)
                                )
                        .get("images/v1/internal/image/direct",
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new GetImageFromImageServerHandler(imageManagerHanlderUtilities,imageManagerDBUtills, cachingImageAccessHandler, imageDatabaseManager),
                                        ADMINISTRATION_REALM_NAME,
                                        hybridIdentityManager)
                                )
                        .get("images/v1/internal/image/deduplicated-id",
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new GetDeduplicatedIdForImageHandler(imageManagerHanlderUtilities,imageManagerDBUtills),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER)
                                )
                        .get("images/v1/image/configuration",
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new GetImageConfigurationDtoHandler(imageManagerHanlderUtilities,imageConfigurationManager),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER)
                                )
        ).addPrefixPath(
                "internal/vpimage/rest",
                Handlers.routing()
                        .post("/publish/images-with-lifetime",
                                addSecurityWithExceptionHandling(
                                serverDatabaseManager,
                                imageDatabaseManager,
                                mhDatabaseManager,
                                forceStrictDatabaseAvailabilityCheck,
                                new PublishImagesWithLifetimeHandler(imageManagerHanlderUtilities),
                                ADMINISTRATION_REALM_NAME,
                                ADMIN_IDENTITY_MANAGER)
                        )
                        .post("/publish/images-without-lifetime",
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new PublishImagesWithoutLifetimeHandler(imageManagerHanlderUtilities),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER)
                        )
                        .get("/image-deduplication/image/lifetime",
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new ImageDeduplicationGetLifetimeForImageLifetimeKeyFromDeduplicatedImageIdHandler(imageDeduplicationCache),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER)
                        )
                        .get("/image-deduplication/cache/deduplication/status-report",
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new ImageDeduplicationGetStatusReportForImageDeduplicationCacheHandler(imageDeduplicationCache),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER)
                        )
                        .get("/image-deduplication/cache/lifetime/status-report",
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new ImageDeduplicationGetStatusReportForImageLifetimeCacheHandler(imageDeduplicationCache),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER)
                        )
                        .delete("/image-deduplication/cache/clear",
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new ImageDeduplicationClearCacheHandler(imageDeduplicationCache),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER)
                        )
                        .get("/image-registry/cache/lifetime/status-report",
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new ImageRegistryGetStatusReportForImageLifetimeCacheHandler(imageRegistry),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER)
                        )
                        .delete("/image-registry/cache/clear",
                                addSecurityWithExceptionHandling(
                                        serverDatabaseManager,
                                        imageDatabaseManager,
                                        mhDatabaseManager,
                                        forceStrictDatabaseAvailabilityCheck,
                                        new ImageRegistryClearCacheHandler(imageRegistry),
                                        ADMINISTRATION_REALM_NAME,
                                        ADMIN_IDENTITY_MANAGER)
                        )
        );
    }

    private static ScheduledExecutorService createAndStartTimerTasks(final DatabaseManager imageDatabaseManager) {

        final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        startImageCleanupTask(scheduledExecutorService, imageDatabaseManager);

        startCacheCleanupTask(scheduledExecutorService);

        return scheduledExecutorService;

    }

    private static void startCacheCleanupTask(final ScheduledExecutorService scheduledExecutorService) {

        final CacheCleanupTask task = new CacheCleanupTask();

        final int scheduledAtHour = 3; // 3 AM

        scheduleTaskOnceADay(scheduledExecutorService, task, scheduledAtHour);

    }

    private static void startImageCleanupTask(final ScheduledExecutorService scheduledExecutorService, final DatabaseManager imageDatabaseManager) {

        final ImageCleanupTask task = new ImageCleanupTask(imageDatabaseManager);

        // TODO: configurable period
        final int scheduledAtHour = 2; // 2 AM (starts between 2:00 and 2:59, based on minute when the server was started)

        scheduleTaskOnceADay(scheduledExecutorService, task, scheduledAtHour);

    }

    /**
     * schedule task once a day between <scheduledAtHour; scheduledAtHour+1)
     * (X:00 to X:59 - based on server start time)
     *
     * @param scheduledExecutorService
     * @param task
     * @param scheduledAtHour
     */
    private static void scheduleTaskOnceADay(final ScheduledExecutorService scheduledExecutorService, final Runnable task, final int scheduledAtHour) {
        final int hoursADay = 24;

        final int delay = scheduledAtHour - LocalTime.now().getHour();

        long initDelay = (delay < 0) ? hoursADay + delay : delay;


        scheduledExecutorService.scheduleAtFixedRate(task, initDelay, hoursADay, TimeUnit.HOURS);
    }

    private static int getHttpPortToBind(final String[] args) {
        return getPortParameter(args, "http-port", DEFAULT_HTTP_PORT);
    }

    private static int getAjpPortToBind(final String[] args) {
        return getPortParameter(args, "ajp-port", DEFAULT_AJP_PORT);
    }

    private static int getPortParameter(final String[] args, String paramName, int defaultPort) {

        try {
            final String httpPort = getParameter(paramName, args);

            if (httpPort != null) {
                final int port = parsePort(httpPort);
                log.info("Provided port ({}) {} will be used", paramName, port);
                return port;
            }

        } catch (NumberFormatException e) {
            log.warn("NumberFormatException for parameter {}: {} - default port {} will be used!", paramName, e.getMessage(), defaultPort);
        }

        return defaultPort;
    }

    private static int parsePort(final String portString) {
        int tempPort = Integer.parseInt(portString);

        if (tempPort < 1024 || tempPort > 0xffff) {
            throw new NumberFormatException("Port " + tempPort + " is not within valid range");
        }
        return tempPort;
    }

    private static String getParameter(String paramName, String[] args) {
        if (args == null || args.length < 2) {
            return null;
        }

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--" + paramName) && ++i < args.length) {
                return args[i];
            }
        }

        return null;

    }

    /**
     * Wrap handler in sort of standard Undertow authentication chain
     * for our simple usage native handlers are completely enough
     *
     * @param mainDatabaseManager
     * @param imageDatabaseManager
     * @param mhDatabaseManager
     * @param forceStrictDatabaseAvailabilityCheck
     * @param toWrap                               handler that needs authentication
     * @param realm                                custom name of the realm to authenticate to
     * @param identityManager                      implementation of authentication and authorization mechanism
     * @return wrapped handler
     */
    private static HttpHandler addSecurityWithExceptionHandling(DatabaseManager mainDatabaseManager, DatabaseManager imageDatabaseManager, DatabaseManager mhDatabaseManager, boolean forceStrictDatabaseAvailabilityCheck, final HttpHandler toWrap, String realm, final IdentityManager identityManager) {

        HttpHandler handler = addExceptionHandling(toWrap);
        handler = new AuthenticationCallHandler(handler);
        handler = new AuthenticationConstraintHandler(handler);
        final List<AuthenticationMechanism> mechanisms = Collections.singletonList(new BasicAuthenticationMechanism(realm));
        handler = new AuthenticationMechanismsHandler(handler, mechanisms);
        handler = new SecurityInitialHandler(AuthenticationMode.PRO_ACTIVE, identityManager, handler);

        if (forceStrictDatabaseAvailabilityCheck) {

            handler = new StrictDatabaseAvailabilityHTTPHandler(mainDatabaseManager, imageDatabaseManager, mhDatabaseManager, handler);
        }

        return handler;
    }

    /**
     * @param mainDatabaseManager
     * @param imageDatabaseManager
     * @param forceStrictDatabaseAvailabilityCheck
     * @param handler
     * @return
     */
    private static HttpHandler addExceptionHandlingWithDispatch(DatabaseManager mainDatabaseManager, DatabaseManager imageDatabaseManager, DatabaseManager mhDatabaseManager, boolean forceStrictDatabaseAvailabilityCheck, final HttpHandler handler) {

        DispatchHandler dispatchHandler = new DispatchHandler(addExceptionHandling(handler));

        return forceStrictDatabaseAvailabilityCheck ? new StrictDatabaseAvailabilityHTTPHandler(mainDatabaseManager, imageDatabaseManager, mhDatabaseManager, dispatchHandler) : dispatchHandler;
    }

    private static ExceptionHandler addExceptionHandling(final HttpHandler handler) {

        final ExceptionHandler exceptionHandler = Handlers.exceptionHandler(handler);

        // Not found
        exceptionHandler.addExceptionHandler(ImageNotFoundException.class, HandlerUtilities::handleNotFound);

        // Bad reguest
        exceptionHandler.addExceptionHandler(ImportWrongImageException.class, HandlerUtilities::handleBadRequest);
        exceptionHandler.addExceptionHandler(IllegalArgumentException.class, HandlerUtilities::handleBadRequest);
        exceptionHandler.addExceptionHandler(DateTimeParseException.class, HandlerUtilities::handleBadRequest);
        exceptionHandler.addExceptionHandler(ImageTypeNotSupportedException.class, HandlerUtilities::handleBadRequest);
        exceptionHandler.addExceptionHandler(MalformedURLException.class, HandlerUtilities::handleBadRequest);
        exceptionHandler.addExceptionHandler(PreconditionException.class, HandlerUtilities::handleBadRequest);
        exceptionHandler.addExceptionHandler(ImageException.class, HandlerUtilities::handleBadRequest);

        // Security
        exceptionHandler.addExceptionHandler(SecurityException.class, HandlerUtilities::handleSecurityException);

        // Internal fatal exceptions
        exceptionHandler.addExceptionHandler(ValuePhoneException.class, HandlerUtilities::handleFatalException);
        exceptionHandler.addExceptionHandler(SQLException.class, HandlerUtilities::handleFatalException);

        // All not listed exceptions will return 500 and mask the message, that is to be logged in the format we expect
        exceptionHandler.addExceptionHandler(Exception.class, HandlerUtilities::handleFatalException);

        return exceptionHandler;
    }

    /**
     * @param environmentPropertyName
     * @param hidePropertyValue
     * @return
     */
    private static String getEnvironmentProperty(String environmentPropertyName, boolean hidePropertyValue) {

        Optional<String> optional = getOptionalEnvironmentProperty(environmentPropertyName, hidePropertyValue);

        return optional.orElseThrow(() -> new FormatedIllegalStateException("Value for environment property %s is not set!", environmentPropertyName));
    }

    /**
     * @param environmentPropertyName
     * @param hidePropertyValue
     * @return
     */
    private static Optional<Integer> getOptionalIntegerEnvironmentProperty(String environmentPropertyName, boolean hidePropertyValue) {

        CheckUtilities.checkArgumentNotNull(environmentPropertyName, "environmentPropertyName");

        String environmentPropertyValue = System.getenv(environmentPropertyName);

        log.debug("Environment property {} = {}", environmentPropertyName, hidePropertyValue ? SecurityUtil.hidePassword(environmentPropertyValue) : environmentPropertyValue);

        if (environmentPropertyValue == null) {
            return Optional.empty();
        }

        try {

            return Optional.of(Integer.parseInt(environmentPropertyValue));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * @param environmentPropertyName
     * @param hidePropertyValue
     * @return
     */
    private static Optional<String> getOptionalEnvironmentProperty(String environmentPropertyName, boolean hidePropertyValue) {

        CheckUtilities.checkArgumentNotNull(environmentPropertyName, "environmentPropertyName");

        String environmentPropertyValue = System.getenv(environmentPropertyName);

        log.debug("Environment property {} = {}", environmentPropertyName, hidePropertyValue ? SecurityUtil.hidePassword(environmentPropertyValue) : environmentPropertyValue);

        return Optional.ofNullable(environmentPropertyValue);
    }
}
