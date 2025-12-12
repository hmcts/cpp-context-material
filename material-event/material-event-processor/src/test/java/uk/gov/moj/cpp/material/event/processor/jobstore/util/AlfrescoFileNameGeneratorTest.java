package uk.gov.moj.cpp.material.event.processor.jobstore.util;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringRegularExpression.matchesRegex;
import static uk.gov.moj.cpp.material.event.processor.jobstore.upload.AlfrescoFileUploader.ALFRESCO_FILE_NAME_CHECKER_REGEX;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AlfrescoFileNameGeneratorTest {

    private final AlfrescoFileNameGenerator alfrescoFileNameGenerator = new AlfrescoFileNameGenerator();

    @Test
    public void shouldGenerateAlfrescoCompliantFileNameWhenFileExtensionIsPresent() {
        final UUID materialId = randomUUID();
        final String fileName = "test.pdf";
        final String generatedFileName = alfrescoFileNameGenerator.generateAlfrescoCompliantFileName(fileName, materialId, "application/pdf");
        assertThat(generatedFileName, notNullValue());
        assertThat(generatedFileName, not(fileName));
        assertThat(generatedFileName, endsWith(".pdf"));
        assertThat(generatedFileName, not(matchesRegex(ALFRESCO_FILE_NAME_CHECKER_REGEX)));

    }

    @Test
    public void shouldGenerateAlfrescoCompliantFileNameWhenFileExtensionIsNotPresentBasedOnMimeType() {
        final UUID materialId = randomUUID();
        final String fileName = "test";
        final String generatedFileName = alfrescoFileNameGenerator.generateAlfrescoCompliantFileName(fileName, materialId, "application/pdf");
        assertThat(generatedFileName, notNullValue());
        assertThat(generatedFileName, not(fileName));
        assertThat(generatedFileName, endsWith(".pdf"));
        assertThat(generatedFileName, not(matchesRegex(ALFRESCO_FILE_NAME_CHECKER_REGEX)));
    }

    @Test
    public void shouldGenerateAlfrescoCompliantFileNameWhenFileExtensionIsPresentAndMimeTyepIsNull() {
        final UUID materialId = randomUUID();
        final String fileName = "test.docx";
        final String generatedFileName = alfrescoFileNameGenerator.generateAlfrescoCompliantFileName(fileName, materialId, null);
        assertThat(generatedFileName, notNullValue());
        assertThat(generatedFileName, not(fileName));
        assertThat(generatedFileName, endsWith(".docx"));
        assertThat(generatedFileName, not(matchesRegex(ALFRESCO_FILE_NAME_CHECKER_REGEX)));
    }


    @Test
    public void shouldValidateAndCorrectFilenameIfFileNameIsNull() {
        final UUID materialId = randomUUID();
        final String fileName = null;
        final String generatedFileName = alfrescoFileNameGenerator.generateAlfrescoCompliantFileName(fileName, materialId, "application/pdf");
        assertThat(generatedFileName, notNullValue());
        assertThat(generatedFileName, not(fileName));
        assertThat(generatedFileName, endsWith(".pdf"));
        assertThat(generatedFileName, not(matchesRegex(ALFRESCO_FILE_NAME_CHECKER_REGEX)));
    }

    @Test
    public void shouldValidateAndCorrectFilenameIfFileNameIsNullAndMimeTypeIsNull() {
        final UUID materialId = randomUUID();
        final String fileName = null;
        final String generatedFileName = alfrescoFileNameGenerator.generateAlfrescoCompliantFileName(fileName, materialId, null);
        assertThat(generatedFileName, notNullValue());
        assertThat(generatedFileName, not(matchesRegex(ALFRESCO_FILE_NAME_CHECKER_REGEX)));
    }

}