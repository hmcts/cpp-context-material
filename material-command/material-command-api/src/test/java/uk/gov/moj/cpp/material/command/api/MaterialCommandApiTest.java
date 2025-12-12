package uk.gov.moj.cpp.material.command.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import org.junit.jupiter.api.Test;

public class MaterialCommandApiTest {

    @Test
    public void shouldHandlePassThroughFromMaterialCommandApiToController() throws Exception {
        assertThat(MaterialCommandApi.class, isHandlerClass(COMMAND_API)
                .with(allOf(
                        method("addMaterial")
                                .thatHandles("material.command.add-material")
                                .withSenderPassThrough(),
                        method("uploadFile")
                                .thatHandles("material.command.upload-file")
                                .withSenderPassThrough(),
                        method("uploadFileAsPdf")
                                .thatHandles("material.command.upload-file-as-pdf")
                                .withSenderPassThrough(),
                        method("deleteMaterial")
                                .thatHandles("material.command.delete-material"),
                        method("addMaterialReference")
                                .thatHandles("material.add-material")
                                .withSenderPassThrough()))
        );
    }
}
