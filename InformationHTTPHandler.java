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
public final class InformationHTTPHandler implements HttpHandler {

    // === Constants ===

    private static final int INFO_CODE = StatusCodes.OK;

    private static final String INFO_PAGE = "<html><body><h1 style=\"color: blue;\">IMAGE APPLICATION WORKS!</h1></body></html>";

    private static final String INFO_PAGE_CONTENT_TYPE = "text/html";

    private static final int ERROR_CODE = StatusCodes.INTERNAL_SERVER_ERROR;

    private static final String ERROR_PAGE = "<html><body><h1 style=\"color: red;\">IMAGE APPLICATION DOES NOT WORK!</h1></body></html>";

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

    // === Constructors ===

    /**
     * @param mainDatabaseManager
     * @param imageDatabaseManager
     */
    public InformationHTTPHandler(DatabaseManager mainDatabaseManager, DatabaseManager imageDatabaseManager) {
        this.mainDatabaseManager = mainDatabaseManager;
        this.imageDatabaseManager = imageDatabaseManager;
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

        if (mainDatabaseAvailable && imageDatabaseAvailable) {

            httpServerExchange.setStatusCode(INFO_CODE);

            HeaderMap responseHeaders = httpServerExchange.getResponseHeaders();

            responseHeaders.put(Headers.CONTENT_TYPE, INFO_PAGE_CONTENT_TYPE);
            responseHeaders.put(Headers.CONTENT_ENCODING, StandardCharsets.UTF_8.displayName());
            responseHeaders.put(Headers.CONTENT_LENGTH, INFO_PAGE.length());

            httpServerExchange.getResponseSender().send(INFO_PAGE);
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
