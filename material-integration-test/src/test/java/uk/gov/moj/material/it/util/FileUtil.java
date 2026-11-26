package uk.gov.moj.material.it.util;

import static org.junit.Assert.fail;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
