package uk.gov.moj.cpp.material.event.processor.jobstore.util;

import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.moj.cpp.material.event.processor.jobstore.util.FileExtensionResolver.getFileExtension;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AlfrescoFileNameGenerator {


    /**
     * Generate new file name for uploading file in alfreso. Alfresco rejects files matching regex -
     * @see uk.gov.moj.cpp.material.event.processor.jobstore.upload.AlfrescoFileUploader#ALFRESCO_FILE_NAME_CHECKER_REGEX
     *
     * @return alfresco compliant file name
     */
    public String generateAlfrescoCompliantFileName(final String fileName, final UUID materialId, final String mimeType) {

        final String fileExtension = getExtension(fileName);
        String newFilename = materialId.toString();

        if (isNotBlank(fileExtension)) {
            newFilename += "." + fileExtension;
        } else if (isNotBlank(mimeType) && isNotBlank(getFileExtension(mimeType))) {
            newFilename += "." + getFileExtension(mimeType);
        }

        return newFilename;
    }
}
