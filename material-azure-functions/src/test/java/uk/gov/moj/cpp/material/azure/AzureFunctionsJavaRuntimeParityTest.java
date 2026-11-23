package uk.gov.moj.cpp.material.azure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
 * are compiled for that version. A Function App can only load them if it runs that Java runtime. The J25 upgrade
 * moves the Function App (deployed on Windows, see the azure-functions-maven-plugin runtime) to Java 25, so pin the
 * J25 class-file level (major version 69). The Function App's runtime must move to Java 25 in the same release as
 * this jar.
 */
class AzureFunctionsJavaRuntimeParityTest {

    private static final int JAVA_25_CLASS_FILE_MAJOR_VERSION = 69;

    @Test
    void maM04_functionClassesShouldTargetTheJava25FunctionAppRuntime() throws IOException {
        assertThat(classFileMajorVersion(LargeFilesCleaner.class), is(JAVA_25_CLASS_FILE_MAJOR_VERSION));
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
