package com.valuephone.image.handler;

import com.valuephone.image.utilities.DatabaseManager;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * @author lbalint
 * @since 1.0
 */
@Slf4j
public final class StrictDatabaseAvailabilityHTTPHandler implements HttpHandler {

    // === Constants ===

    private static final int ERROR_CODE = StatusCodes.INTERNAL_SERVER_ERROR;

    private static final String ERROR_PAGE = "<html><body><h1 style=\"color: red;\">IMAGE APPLICATION DATABASES AVAILABILITY ERROR!</h1></body></html>";

    private static final String ERROR_PAGE_CONTENT_TYPE = "text/html";

    // === Attributes ===

    /**
     *
     */
    private final DatabaseManager mainDatabaseManager;

    /**
     *
     */
    private final DatabaseManager imageDatabaseManager;

    private DatabaseManager mhDatabaseManager;
    /**
     *
     */
    private final HttpHandler nextHTTPHandler;

    // === Constructors ===

    /**
     * @param mainDatabaseManager
     * @param imageDatabaseManager
     * @param mhDatabaseManager
     * @param nextHTTPHandler
     */
    public StrictDatabaseAvailabilityHTTPHandler(DatabaseManager mainDatabaseManager, DatabaseManager imageDatabaseManager, DatabaseManager mhDatabaseManager, HttpHandler nextHTTPHandler) {
        this.mainDatabaseManager = mainDatabaseManager;
        this.imageDatabaseManager = imageDatabaseManager;
        this.mhDatabaseManager = mhDatabaseManager;
        this.nextHTTPHandler = nextHTTPHandler;
    }

    // === Implemented methods ===

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {

        String mainDatabaseIdentifier = mainDatabaseManager.getIdentifier();

        boolean mainDatabaseAvailable = mainDatabaseManager.isAlive();

        log.debug("Main database {} available: {}", mainDatabaseIdentifier, mainDatabaseAvailable);

        String imageDatabaseIdentifier = imageDatabaseManager.getIdentifier();

        boolean imageDatabaseAvailable = imageDatabaseManager.isAlive();

        log.debug("Image database {} available: {}", imageDatabaseIdentifier, imageDatabaseAvailable);

        String imageManagerDatabaseIdentifier = mhDatabaseManager.getIdentifier();

        boolean imageManagerDatabaseAvailable = mhDatabaseManager.isAlive();

        log.debug("Image manager database {} available: {}", imageManagerDatabaseIdentifier, imageManagerDatabaseAvailable);

        if (mainDatabaseAvailable && imageDatabaseAvailable && imageManagerDatabaseAvailable) {

            nextHTTPHandler.handleRequest(httpServerExchange);
        } else {

            httpServerExchange.setStatusCode(ERROR_CODE);

            HeaderMap responseHeaders = httpServerExchange.getResponseHeaders();

            responseHeaders.put(Headers.CONTENT_TYPE, ERROR_PAGE_CONTENT_TYPE);
            responseHeaders.put(Headers.CONTENT_ENCODING, StandardCharsets.UTF_8.displayName());
            responseHeaders.put(Headers.CONTENT_LENGTH, ERROR_PAGE.length());

            httpServerExchange.getResponseSender().send(ERROR_PAGE);
        }
    }
}
