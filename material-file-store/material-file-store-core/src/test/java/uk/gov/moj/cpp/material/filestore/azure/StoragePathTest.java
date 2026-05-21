package uk.gov.moj.cpp.material.filestore.azure;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class StoragePathTest {

    @Test
    void shouldReturnInternalBlobName() {
        final UUID fileId = randomUUID();
        assertThat(StoragePath.internal().blobName(fileId), is("internal/" + fileId));
    }

    @Test
    void shouldReturnPublishedBlobName() {
        final UUID fileId = randomUUID();
        assertThat(StoragePath.published("sjp").blobName(fileId), is("published/sjp/" + fileId));
    }

    @Test
    void shouldThrowWhenPublishedTopicIsNull() {
        assertThrows(NullPointerException.class, () -> StoragePath.published(null));
    }

    @Test
    void shouldReturnPrefix() {
        assertThat(StoragePath.internal().prefix(), is("internal"));
        assertThat(StoragePath.published("topic").prefix(), is("published/topic"));
    }

    @Test
    void shouldReturnToString() {
        assertThat(StoragePath.internal().toString(), is("internal"));
    }

    @Test
    void shouldBeEqualForSamePrefix() {
        assertThat(StoragePath.internal(), is(StoragePath.internal()));
    }

    @Test
    void shouldNotBeEqualForDifferentPrefixes() {
        assertThat(StoragePath.internal(), is(not(StoragePath.published("x"))));
    }

    @Test
    void shouldHaveSameHashCodeForSamePrefix() {
        assertThat(StoragePath.internal().hashCode(), is(StoragePath.internal().hashCode()));
    }

    @Test
    void shouldNotEqualNull() {
        assertThat(StoragePath.internal().equals(null), is(false));
    }

    @Test
    void shouldNotEqualDifferentType() {
        assertThat(StoragePath.internal().equals("internal"), is(false));
    }

    @Test
    void shouldEqualSelf() {
        final StoragePath path = StoragePath.internal();
        assertThat(path.equals(path), is(true));
    }
}
