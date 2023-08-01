package com.valuephone.image.management.images;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Created with IntelliJ IDEA.
 * <p>
 * User: ahack Date: 20.05.16 Time: 09:14
 */
@Slf4j
public class ImageDownloadHelper {
    private static final String HTTP_AGENT = "MCA";

    public ImageDownloadResult downloadImageFromUrl(URL imageUrl) throws IOException {
        OffsetDateTime modificationDate;
        HttpURLConnection httpCon = null;
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            int expectedDataSize = -1;

            System.setProperty("http.agent", HTTP_AGENT);
            httpCon = (HttpURLConnection) imageUrl.openConnection();

            httpCon.setRequestMethod("GET");
            modificationDate = OffsetDateTime.ofInstant(Instant.ofEpochMilli(httpCon.getLastModified()), ZoneId.systemDefault());

            String contentLength = httpCon.getHeaderField("Content-Length");
            if (contentLength != null && !contentLength.isEmpty())
                expectedDataSize = Integer.valueOf(contentLength);

            try (InputStream is = httpCon.getInputStream()) {
                byte[] byteChunk = new byte[4096];
                int n;

                while ((n = is.read(byteChunk)) > 0) {
                    os.write(byteChunk, 0, n);
                }
            } catch (IOException e) {
                log.error("Failed while reading bytes from " + imageUrl.toExternalForm());
                throw new IOException(e);
            }

            byte[] dataBytes = os.toByteArray();
            if (dataBytes.length != expectedDataSize) {
                log.warn("mismatching size[" + dataBytes.length + "] expected[" + expectedDataSize +
                        "] for image[" + imageUrl + "]");
            }

            return new ImageDownloadResult(dataBytes, modificationDate);
        } finally {
            if (httpCon != null)
                httpCon.disconnect();
        }
    }
}
