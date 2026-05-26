package uk.gov.moj.cpp.material.filestore.azure;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Holds a file retrieved from Azure Blob Storage.
 *
 * <p><strong>Resource lifecycle:</strong> {@link #getInputStream()} returns a <em>live</em>
 * {@code BlobInputStream} backed by an open HTTP connection to Azure Blob Storage. The caller
 * is responsible for closing this stream after use — failure to do so will leak the underlying
 * connection. Use try-with-resources wherever possible:
 *
 * <pre>{@code
 * try (StoredFile storedFile = storageFileRetriever.retrieve(path, fileId).orElseThrow()) {
 *     process(storedFile.getInputStream());
 * }
 * }</pre>
 *
 * <p>Metadata keys follow snake_case convention: {@code "filename"} and {@code "media_type"}.
 */
public class StoredFile implements AutoCloseable {

    private final InputStream trackingStream;
    private final Map<String, String> metadata;
    private volatile boolean closed = false;

    /**
     * Wraps {@code inputStream} in a tracking delegate so that closing the stream via any
     * path — {@link #close()} or directly via the returned stream — marks this instance closed.
     */
    public StoredFile(final InputStream inputStream, final Map<String, String> metadata) {
        this.metadata = metadata;
        this.trackingStream = new FilterInputStream(inputStream) {
            @Override
            public void close() throws IOException {
                closed = true;
                super.close();
            }
        };
    }

    /**
     * Returns the live input stream for this blob.
     *
     * <p>The stream must be closed by the caller. Prefer {@link StoredFile} in a
     * try-with-resources block rather than closing the stream directly.
     *
     * @return the blob input stream; never {@code null}
     * @throws IllegalStateException if this {@code StoredFile} has already been closed —
     *         whether via {@link #close()} or by calling {@code close()} on the stream itself
     */
    public InputStream getInputStream() {
        if (closed) {
            throw new IllegalStateException("StoredFile has already been closed — the underlying stream is no longer available");
        }
        return trackingStream;
    }

    /**
     * Returns the blob metadata map.
     *
     * <p>Keys use snake_case: {@code "filename"}, {@code "media_type"}.
     *
     * @return an unmodifiable view of the metadata; never {@code null}
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * Closes the underlying input stream, releasing the Azure Blob Storage connection.
     * After this call, {@link #getInputStream()} will throw {@link IllegalStateException}.
     *
     * @throws IOException if the stream cannot be closed
     */
    @Override
    public void close() throws IOException {
        trackingStream.close();
    }
}
