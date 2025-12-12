package uk.gov.justice.api.resource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import uk.gov.justice.services.core.accesscontrol.AccessControlFailureMessageGenerator;
import uk.gov.justice.services.core.accesscontrol.AccessControlService;
import uk.gov.justice.services.core.accesscontrol.AccessControlViolation;
import uk.gov.justice.services.core.audit.AuditService;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.query.service.AlfrescoReadService;
import uk.gov.moj.cpp.material.query.service.AzureBlobClientService;
import uk.gov.moj.cpp.material.query.view.MaterialView;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.util.List;
import java.util.UUID;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;

@SuppressWarnings("deprecation")
@ExtendWith(MockitoExtension.class)
public class DefaultQueryApiMaterialMaterialIdResourceTest {

    @Mock
    private AlfrescoReadService alfrescoReadService;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private AccessControlFailureMessageGenerator accessControlFailureMessageGenerator;

    @Mock
    private MaterialEnvelopeFactory materialEnvelopeFactory;

    @Mock
    private AuditService auditService;

    @Mock
    private Logger logger;

    @Mock
    private AzureBlobClientService azureBlobClientService;

    @InjectMocks
    private DefaultQueryApiMaterialMaterialIdResource defaultMaterialMaterialIdResource;
    @Captor
    private ArgumentCaptor<String> destinationFileNameCaptor;

    @Test
    public void shouldGetInputStreamToTheFileServerAndUseItToCreateTheResponse() throws Exception {

        final UUID materialId = randomUUID();
        final String userId = "userId";
        final String contentType = "content/type";
        final InputStream documentInputStream = new StringBufferInputStream("document");

        final JsonEnvelope jsonEnvelopeForMaterialQuery = mock(JsonEnvelope.class);
        final MaterialView materialView = mock(MaterialView.class);

        when(materialEnvelopeFactory.buildEnvelope(
                userId,
                materialId.toString())).thenReturn(jsonEnvelopeForMaterialQuery);

        when(accessControlService.checkAccessControl(QUERY_API, jsonEnvelopeForMaterialQuery)).thenReturn(empty());

        when(alfrescoReadService.getDataById(materialId)).thenReturn(of(materialView));

        when(materialView.getDocumentInputStream()).thenReturn(documentInputStream);
        when(materialView.getContentType()).thenReturn(contentType);
        when(azureBlobClientService.upload(any(), any(), any())).thenReturn("https://sasteccmdd4196.blob.core.windows.net/alfresco-blob-container/test.txt");

        final Response response = defaultMaterialMaterialIdResource.getMaterialByMaterialId(materialId.toString(), "true", userId);

        assertThat(response.getStatus(), is(OK.getStatusCode()));
        assertThat(response.getEntity(), is("https://sasteccmdd4196.blob.core.windows.net/alfresco-blob-container/test.txt"));

        verify(auditService).audit(jsonEnvelopeForMaterialQuery, QUERY_API);
    }

    @Test
    public void shouldReturnNotFoundIfNoFileFoundOnTheFileServer() throws Exception {

        final UUID materialId = randomUUID();
        final String userId = "userId";

        final JsonEnvelope jsonEnvelopeForMaterialQuery = mock(JsonEnvelope.class);

        when(materialEnvelopeFactory.buildEnvelope(
                userId,
                materialId.toString())).thenReturn(jsonEnvelopeForMaterialQuery);

        when(accessControlService.checkAccessControl(QUERY_API, jsonEnvelopeForMaterialQuery)).thenReturn(empty());

        when(alfrescoReadService.getDataById(materialId)).thenReturn(empty());


        final Response response = defaultMaterialMaterialIdResource.getMaterialByMaterialId(materialId.toString(), "true", userId);

        assertThat(response.getStatus(), is(NOT_FOUND.getStatusCode()));

        verify(auditService).audit(jsonEnvelopeForMaterialQuery, QUERY_API);
    }

    @Test
    public void shouldReturn403ForbiddenIfAccessControlForbidsTheRequest() throws Exception {

        final UUID materialId = randomUUID();
        final String userId = "userId";
        final String errorMessage = "oh dear";

        final JsonEnvelope jsonEnvelopeForMaterialQuery = mock(JsonEnvelope.class);
        final AccessControlViolation accessControlViolation = mock(AccessControlViolation.class);

        when(materialEnvelopeFactory.buildEnvelope(
                userId,
                materialId.toString())).thenReturn(jsonEnvelopeForMaterialQuery);

        when(accessControlService.checkAccessControl(QUERY_API, jsonEnvelopeForMaterialQuery)).thenReturn(of(accessControlViolation));
        when(accessControlFailureMessageGenerator.errorMessageFrom(
                jsonEnvelopeForMaterialQuery,
                accessControlViolation)).thenReturn(errorMessage);

        final Response response = defaultMaterialMaterialIdResource.getMaterialByMaterialId(materialId.toString(), "true", userId);

        assertThat(response.getStatus(), is(FORBIDDEN.getStatusCode()));
        final String errorJson = response.getEntity().toString();

        with(errorJson).assertThat("$.error", is(errorMessage));

        verify(auditService).audit(jsonEnvelopeForMaterialQuery, QUERY_API);
    }

    @Test
    public void givenTwoConcurrentRequestsUploadingSameMaterialToAzureBlobAtTheSameTimeShouldUploadToTwoDifferentLocations() {

        final UUID materialId = randomUUID();
        final String userId = "userId";
        final String contentType = "content/type";
        final InputStream documentInputStream = new StringBufferInputStream("document");

        final JsonEnvelope jsonEnvelopeForMaterialQuery = mock(JsonEnvelope.class);
        final MaterialView materialView = mock(MaterialView.class);

        when(materialEnvelopeFactory.buildEnvelope(
                userId,
                materialId.toString())).thenReturn(jsonEnvelopeForMaterialQuery);

        when(accessControlService.checkAccessControl(QUERY_API, jsonEnvelopeForMaterialQuery)).thenReturn(empty());

        when(alfrescoReadService.getDataById(materialId)).thenReturn(of(materialView));
        when(materialView.getFileName()).thenReturn("test.pdf");

        when(materialView.getDocumentInputStream()).thenReturn(documentInputStream);
        when(materialView.getContentType()).thenReturn(contentType);
        when(azureBlobClientService.upload(any(), any(), any())).thenReturn("https://sasteccmdd4196.blob.core.windows.net/alfresco-blob-container/1/test.txt",
                "https://sasteccmdd4196.blob.core.windows.net/alfresco-blob-container/2/test.txt");

        final Response response1 = defaultMaterialMaterialIdResource.getMaterialByMaterialId(materialId.toString(), "true", userId);
        final Response response2 = defaultMaterialMaterialIdResource.getMaterialByMaterialId(materialId.toString(), "true", userId);

        assertThat(response1.getStatus(), is(OK.getStatusCode()));
        assertThat(response1.getLocation().toString(), is("https://sasteccmdd4196.blob.core.windows.net/alfresco-blob-container/1/test.txt"));
        assertThat(response2.getStatus(), is(OK.getStatusCode()));
        assertThat(response2.getLocation().toString(), is("https://sasteccmdd4196.blob.core.windows.net/alfresco-blob-container/2/test.txt"));

        verify(azureBlobClientService, times(2)).upload(any(), destinationFileNameCaptor.capture(), anyString());

        final List<String> destinationFileNames = destinationFileNameCaptor.getAllValues();
        assertThat(destinationFileNames.size(), is(2));
        assertThat(destinationFileNames.get(0).equals(destinationFileNames.get(1)), is(false));
    }
}
