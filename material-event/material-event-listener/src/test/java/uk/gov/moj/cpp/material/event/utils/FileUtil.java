package uk.gov.moj.cpp.material.event.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Utility class for reading json response from a file.
 */
public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

    public static String getPayload(String path) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(path),
                    Charset.defaultCharset()
            );
        } catch (Exception e) {
            LOGGER.error(String.format("Error consuming file from location {}", path), e);
            fail("Error consuming file from location " + path);
        }
        return request;
    }

    public static JsonObject givenPayload(String filePath) throws IOException {
        try (InputStream inputStream = FileUtil.class.getResourceAsStream(filePath)) {
            JsonReader jsonReader = Json.createReader(inputStream);
            return jsonReader.readObject();
        }
    }

    public static byte[] getDocumentBytesFromFile(String filepath) {
        byte[] documentBytes;
        try {
            documentBytes = Files.readAllBytes(Paths.get(ClassLoader.getSystemResource(filepath).toURI()));
        } catch (Exception e) {
            LOGGER.error("Error reading file from location " + filepath, e);
            throw new RuntimeException("Error reading file from location " + filepath);
        }

        return documentBytes;
    }

}
