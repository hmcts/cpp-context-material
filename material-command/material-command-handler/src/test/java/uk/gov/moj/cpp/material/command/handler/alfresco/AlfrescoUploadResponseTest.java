package uk.gov.moj.cpp.material.command.handler.alfresco;

import com.google.common.testing.EqualsTester;
import org.junit.jupiter.api.Test;

public class AlfrescoUploadResponseTest {

    @SuppressWarnings({"squid:MethodCyclomaticComplexity", "squid:S1067", "squid:S00122"})
    @Test
    public void equalsAndHashCode() {
        AlfrescoStatus status = new AlfrescoStatus(1, "name", "description");
        AlfrescoUploadResponse item1 = new AlfrescoUploadResponse("nodeRef", "fileName", "text/plain", status);
        AlfrescoUploadResponse item2 = new AlfrescoUploadResponse("nodeRef", "fileName", "text/plain", status);
        AlfrescoUploadResponse item3 = new AlfrescoUploadResponse("anotherNodeRef", "fileName", "text/plain", status);
        AlfrescoUploadResponse item4 = new AlfrescoUploadResponse("nodeRef", "AnotherFileName", "text/plain", status);
        AlfrescoUploadResponse item5 = new AlfrescoUploadResponse("nodeRef", "filename", "application/xml", status);
        AlfrescoUploadResponse item6 = new AlfrescoUploadResponse("nodeRef", "fileName", "text/plain", new AlfrescoStatus(2, "name2", "description2"));
        AlfrescoUploadResponse item7 = new AlfrescoUploadResponse("nodeRef", null, null, null);

        new EqualsTester()
                .addEqualityGroup(item1, item2)
                .addEqualityGroup(item3)
                .addEqualityGroup(item4)
                .addEqualityGroup(item5)
                .addEqualityGroup(item6)
                .addEqualityGroup(item7)
                .testEquals();
    }
}