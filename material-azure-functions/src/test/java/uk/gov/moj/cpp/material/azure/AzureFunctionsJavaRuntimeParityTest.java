package uk.gov.moj.cpp.material.azure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import uk.gov.moj.cpp.material.azure.function.LargeFilesCleaner;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

/**
 * J17/J25 parity characterisation (J17 golden master), MA-M04.
 *
 * This module is deployed on its own as an Azure Function App (LargeFilesCleaner). Its runtime javaVersion and
 * JAVA_VERSION app setting come from java.se.version, which is 17 on main and 25 after the upgrade, and the classes
 * are compiled for that version. A Function App can only load them if Azure Functions offers that Java runtime, so
 * pin the J17 class-file level (major version 61).
 */
class AzureFunctionsJavaRuntimeParityTest {

    private static final int JAVA_17_CLASS_FILE_MAJOR_VERSION = 61;

    @Test
    void maM04_functionClassesShouldBeLoadableByAJava17FunctionAppRuntime() throws IOException {
        assertThat(classFileMajorVersion(LargeFilesCleaner.class), lessThanOrEqualTo(JAVA_17_CLASS_FILE_MAJOR_VERSION));
    }

    private static int classFileMajorVersion(final Class<?> type) throws IOException {
        try (InputStream classFile = type.getResourceAsStream(type.getSimpleName() + ".class");
             DataInputStream data = new DataInputStream(classFile)) {
            data.readInt();
            data.readUnsignedShort();
            return data.readUnsignedShort();
        }
    }
}
