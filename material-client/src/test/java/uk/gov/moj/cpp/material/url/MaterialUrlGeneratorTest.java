package uk.gov.moj.cpp.material.url;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.material.MaterialUrls.BASE_URI;
import static uk.gov.moj.cpp.material.MaterialUrls.MATERIAL_REQUEST_PATH;
import static uk.gov.moj.cpp.material.MaterialUrls.MATERIAL_STREAM_PDF_PARAMETERS;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialUrlGeneratorTest {

    @InjectMocks
    private MaterialUrlGenerator materialUrlGenerator;

    @Test
    public void shouldGeneratePdfMaterialUrlStringWithGivenMaterialId() {
        final UUID materialId = randomUUID();
        final String expectedUrl = BASE_URI + MATERIAL_REQUEST_PATH + materialId + MATERIAL_STREAM_PDF_PARAMETERS;

        final String actualUrl = materialUrlGenerator.pdfFileStreamUrlFor(materialId);

        assertThat(actualUrl, is(expectedUrl));
    }

    @Test
    public void shouldGenerateNonPdfMaterialUrlStringWithGivenMaterialId() {
        final UUID materialId = randomUUID();
        final String expectedUrl = BASE_URI + MATERIAL_REQUEST_PATH + materialId;

        final String actualUrl = materialUrlGenerator.fileStreamUrlFor(materialId);

        assertThat(actualUrl, is(expectedUrl));
    }

    @Test
    public void shouldGeneratePdfMaterialUrlStringWithGivenMaterialIdWithPdfFlag() {
        final UUID materialId = randomUUID();
        final String expectedUrl = BASE_URI + MATERIAL_REQUEST_PATH + materialId + MATERIAL_STREAM_PDF_PARAMETERS;

        final String actualUrl = materialUrlGenerator.fileStreamUrlFor(materialId, true);

        assertThat(actualUrl, is(expectedUrl));
    }

    @Test
    public void shouldGenerateNonPdfMaterialUrlStringWithGivenMaterialIdWithNonPdfFlag() {
        final UUID materialId = randomUUID();
        final String expectedUrl = BASE_URI + MATERIAL_REQUEST_PATH + materialId;

        final String actualUrl = materialUrlGenerator.fileStreamUrlFor(materialId, false);

        assertThat(actualUrl, is(expectedUrl));
    }

}
