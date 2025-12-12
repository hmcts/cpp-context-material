package uk.gov.moj.cpp.material.event.processor.jobstore.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static uk.gov.moj.cpp.material.event.processor.jobstore.util.FileExtensionResolver.getFileExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FileExtensionResolverTest {

    @Test
    public void shouldReturnValidExtensionForValidMimeType() {
        String result = getFileExtension("application/pdf");
        assertThat(result, is("pdf"));
    }

    @Test
    public void shouldReturnEmptyStringForInValidMimeType() {
        String result = getFileExtension("invalid_mime_type");
        assertThat(result, isEmptyOrNullString());
    }
}