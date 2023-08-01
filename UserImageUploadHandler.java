package com.valuephone.image.handler;

import com.valuephone.image.dto.ImageMimeType;
import com.valuephone.image.exception.ImageException;
import com.valuephone.image.exception.ImageTypeNotSupportedException;
import com.valuephone.image.helper.CustomImageReader;
import com.valuephone.image.security.ImageUserPrincipal;
import com.valuephone.image.utilities.Constants;
import com.valuephone.image.utilities.DatabaseManager;
import io.undertow.server.BlockingHttpExchange;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.util.HeaderValues;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * @author tcigler
 * @since 1.0
 */
@Slf4j
public class UserImageUploadHandler extends AuthenticatedUserWrapperHandler {

    private final DatabaseManager imageDatabaseManager;

    public UserImageUploadHandler(final DatabaseManager imageDatabaseManager) {
        this.imageDatabaseManager = imageDatabaseManager;
    }

    @Override
    public void handleRequestAuthenticated(final HttpServerExchange exchange, ImageUserPrincipal principal) throws Exception {

        try(BlockingHttpExchange ignored = exchange.startBlocking();CloseablePath uploadedFile = getFirstFileFromMultipartRequest(exchange)) {

            final HeaderValues publiclyAccessibleHeaderValue = exchange.getRequestHeaders().get(Constants.HEADER_PUBLICLY_ACCESSIBLE);

            final boolean publiclyAccessible = Optional.ofNullable(publiclyAccessibleHeaderValue)
                    .map(HeaderValues::getFirst).map(Boolean::valueOf)
                    .orElse(false); // image is private by default

            final CustomImageReader reader;
            try (InputStream imageIS = Files.newInputStream(uploadedFile.getPath())) {

                reader = new CustomImageReader(imageIS);

                try (Connection connection = imageDatabaseManager.getConnection()) {

                    final long storedImageId = storeUserImage(connection, principal, reader.getImageBytes(), reader.getMimeType(), publiclyAccessible);

                    exchange.getResponseSender().send(String.format("id=%d", storedImageId), StandardCharsets.UTF_8);

                }
            }
        }

    }

    private static class CloseablePath implements Closeable {

        private Path path;
        private Closeable closeableObject;

        public CloseablePath(Path path, Closeable closeableObject) {
            this.path = path;
            this.closeableObject = closeableObject;
        }

        public Path getPath() {
            return path;
        }

        @Override
        public void close() throws IOException {
            closeableObject.close();
        }
    }

    private CloseablePath getFirstFileFromMultipartRequest(final HttpServerExchange exchange) throws IOException, ImageTypeNotSupportedException {
        final FormParserFactory factory = FormParserFactory.builder().build();
        final FormDataParser parser = factory.createParser(exchange);

        final FormData formData = parser.parseBlocking();

        Path uploadedFile = null;

        for (final String formPartKey : formData) {

            final FormData.FormValue value = formData.getFirst(formPartKey);

            if (value.isFileItem()) {

                FormData.FileItem fileItem = value.getFileItem();

                log.debug("File item: {}", fileItem);

                Path file = fileItem.getFile();

                log.debug("File: {}", file);

                uploadedFile = file;

                log.debug("Multipart form contains a file in part {}", formPartKey);

                break;
            }
        }

        if (uploadedFile == null) {
            throw new ImageTypeNotSupportedException("Multipart form does not contain any file to store!");
        }

        return new CloseablePath(uploadedFile, parser);
    }

    private long storeUserImage(final Connection connection, ImageUserPrincipal principal, byte[] imageBytes, ImageMimeType mimeType, boolean isPublic) throws ImageException {

        try (final PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO user_images (user_id, mime_type, content, public) VALUES (?, ?, ?, ?) RETURNING id")
        ) {

            int x = 1;

            preparedStatement.setInt(x++, principal.getUserId());
            preparedStatement.setString(x++, mimeType.toString());
            preparedStatement.setBytes(x++, imageBytes);
            preparedStatement.setBoolean(x, isPublic);

            try (ResultSet returning = preparedStatement.executeQuery()) {
                if (!returning.next()) {
                    throw new ImageException("Could not save user image");
                }
                return returning.getLong(1);
            }

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException("SQL Exception!");
        }

    }


}
