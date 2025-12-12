package uk.gov.moj.cpp.material.command.handler.alfresco;

import com.google.common.testing.EqualsTester;
import org.junit.jupiter.api.Test;

public class AlfrescoStatusTest {

    @SuppressWarnings({"squid:MethodCyclomaticComplexity", "squid:S1067", "squid:S00122"})
    @Test
    public void equalsAndHashCode() {
        AlfrescoStatus item1 = new AlfrescoStatus(1, "name", "description");
        AlfrescoStatus item2 = new AlfrescoStatus(1, "name", "description");
        AlfrescoStatus item3 = new AlfrescoStatus(2, "name", "description");
        AlfrescoStatus item4 = new AlfrescoStatus(1, "AnotherName", "description");
        AlfrescoStatus item5 = new AlfrescoStatus(1, "name", "AnotherDescription");
        AlfrescoStatus item6 = new AlfrescoStatus(1, null, null);

        new EqualsTester()
                .addEqualityGroup(item1, item2)
                .addEqualityGroup(item3)
                .addEqualityGroup(item4)
                .addEqualityGroup(item5)
                .addEqualityGroup(item6)
                .testEquals();
    }
}