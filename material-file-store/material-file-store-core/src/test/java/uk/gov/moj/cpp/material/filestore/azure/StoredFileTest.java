package uk.gov.moj.cpp.material.filestore.azure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

class StoredFileTest {

    @Test
    void shouldReturnReadableStreamBeforeClose() throws IOException {
        final byte[] content = {1, 2, 3};
        final StoredFile storedFile = new StoredFile(new ByteArrayInputStream(content), Map.of("filename", "test.pdf"));

        assertThat(storedFile.getInputStream().read(), is(1));
    }

    @Test
    void shouldReturnMetadata() {
        final Map<String, String> metadata = Map.of("filename", "test.pdf", "media_type", "application/pdf");
        final StoredFile storedFile = new StoredFile(mock(InputStream.class), metadata);

        assertThat(storedFile.getMetadata(), is(metadata));
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenGetInputStreamCalledAfterStoredFileClose() throws IOException {
        final StoredFile storedFile = new StoredFile(mock(InputStream.class), Map.of());

        storedFile.close();

        assertThrows(IllegalStateException.class, storedFile::getInputStream);
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenGetInputStreamCalledAfterDirectStreamClose() throws IOException {
        final StoredFile storedFile = new StoredFile(mock(InputStream.class), Map.of());

        storedFile.getInputStream().close();

        assertThrows(IllegalStateException.class, storedFile::getInputStream);
    }

    @Test
    void shouldCloseUnderlyingInputStream() throws IOException {
        final InputStream inputStream = mock(InputStream.class);
        final StoredFile storedFile = new StoredFile(inputStream, Map.of());

        storedFile.close();

        verify(inputStream).close();
    }
}
