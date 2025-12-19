package uk.gov.moj.cpp.material.command.handler.alfresco;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import uk.gov.moj.cpp.material.command.handler.alfresco.exception.AlfrescoUploadException;
import uk.gov.moj.cpp.material.command.handler.client.RestClient;
import uk.gov.moj.cpp.material.domain.UploadedMaterial;

import java.io.IOException;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AlfrescoUploadServiceTest {

    private static final UUID MATERIAL_ID = UUID.randomUUID();
    private static final String MATERIAL_ID_NAME = "materialId";
    private static final String FILENAME = "MaterialTestFile.txt";
    private static final String DOCUMENT_DATA = "aGVsbG8=";

    private static final String ALFRESCO_UPLOAD_PATH = "/upload/";
    private static final String ALFRESCO_BASE_URI = "http://alfresco/test";
    private static final String ALFRESCO_UPLOAD_USER = "testuploaduser";

    private static final int STATUS_CODE = Response.Status.FORBIDDEN.getStatusCode();
    private static final Response.Status STATUS_INFO = Response.Status.FORBIDDEN;

    private static final String WORKSPACE = "workspace://SpacesStore/";
    private static final String ALFRESCO_ASSET_ID = "e8fb47c8-056e-11e6-b512-3e1d05defe77";
    private static final String ALFRESCO_MIME_TYPE = "text/plain";

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RestClient restClient;

    @InjectMocks
    private AlfrescoUploadService alfrescoUploadService;

    @Mock
    private Response response;

    @Mock
    private AlfrescoUploadResponse alfrescoUploadResponse;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setup() {
        alfrescoUploadService.alfrescoBaseUri = ALFRESCO_BASE_URI;
        alfrescoUploadService.alfrescoUploadPath = ALFRESCO_UPLOAD_PATH;
        alfrescoUploadService.alfrescoUploadUser = ALFRESCO_UPLOAD_USER;
    }

    @Test
    public void shouldUploadFile() throws IOException {
        String uploadResponse = "{\n" +
                "  \"nodeRef\": \"" + WORKSPACE + ALFRESCO_ASSET_ID + "\",\n" +
                "  \"fileName\": \"test.txt\",\n" +
                "  \"fileMimeType\": \"" + ALFRESCO_MIME_TYPE + "\",\n" +
                "  \"status\": {\n" +
                "    \"code\": \"200\",\n" +
                "    \"name\": \"OK\",\n" +
                "    \"description\": \"Success\"\n" +
                "  }\n" +
                "}";

        given(restClient.post(eq(ALFRESCO_BASE_URI + ALFRESCO_UPLOAD_PATH), eq(MediaType.MULTIPART_FORM_DATA_TYPE), Mockito.any(MultivaluedHashMap.class), Mockito.any(Entity.class))).willReturn(response);given(response.readEntity(String.class)).willReturn(uploadResponse);
        given(response.getStatus()).willReturn(Response.Status.OK.getStatusCode());
        given(objectMapper.readValue(uploadResponse, AlfrescoUploadResponse.class)).willReturn(alfrescoUploadResponse);
        given(alfrescoUploadResponse.getNodeRef()).willReturn(WORKSPACE + ALFRESCO_ASSET_ID);
        given(alfrescoUploadResponse.getFileMimeType()).willReturn(ALFRESCO_MIME_TYPE);

        UploadedMaterial uploadedMaterial = alfrescoUploadService.uploadFile(buildRequestData());

        assertThat(uploadedMaterial.getExternalId(), equalTo(ALFRESCO_ASSET_ID));
        assertThat(uploadedMaterial.getMimeType(), equalTo(ALFRESCO_MIME_TYPE));
        verify(response).readEntity(eq(String.class));
        verify(response).getStatus();
        verify(objectMapper).readValue(eq(uploadResponse), eq(AlfrescoUploadResponse.class));
        verify(alfrescoUploadResponse).getNodeRef();
        verify(alfrescoUploadResponse).getFileMimeType();
    }

    @Test

    public void shouldThrowExceptionOnFailedUploadFileDueToNullEntity() throws IOException {

        final JsonObject addMaterialCommand = buildRequestData();

        given(restClient.post(eq(ALFRESCO_BASE_URI + ALFRESCO_UPLOAD_PATH), eq(MediaType.MULTIPART_FORM_DATA_TYPE), Mockito.any(MultivaluedHashMap.class), Mockito.any(Entity.class))).willReturn(response);given(response.readEntity(String.class)).willReturn(null);
        given(response.getStatus()).willReturn(Response.Status.ACCEPTED.getStatusCode());
        given(response.getStatusInfo()).willReturn(Response.Status.ACCEPTED);

        try {
            assertThrows(AlfrescoUploadException.class, () -> alfrescoUploadService.uploadFile(addMaterialCommand));
        } finally {
            verify(response).readEntity(eq(String.class));
            verify(response, times(2)).getStatus();
            verify(response).getStatusInfo();
        }
    }

    @Test
    public void shouldThrowExceptionOnFailedUploadFileDueToNonSuccessStatus() throws IOException {

        final JsonObject addMaterialCommand = buildRequestData();

        given(restClient.post(eq(ALFRESCO_BASE_URI + ALFRESCO_UPLOAD_PATH), eq(MediaType.MULTIPART_FORM_DATA_TYPE), Mockito.any(MultivaluedHashMap.class), Mockito.any(Entity.class))).willReturn(response);given(response.readEntity(String.class)).willReturn("result");
        given(response.getStatus()).willReturn(STATUS_CODE);
        given(response.getStatusInfo()).willReturn(STATUS_INFO);

        try {
            assertThrows(AlfrescoUploadException.class, () -> alfrescoUploadService.uploadFile(addMaterialCommand));
        } finally {
            verify(response).readEntity(eq(String.class));
            verify(response, times(2)).getStatus();
            verify(response).getStatusInfo();
        }
    }

    @Test
    public void shouldThrowExceptionIfNoDocument() throws IOException {
        JsonObject command = JsonObjects.createObjectBuilder()
                .add(MATERIAL_ID_NAME, MATERIAL_ID.toString())
                .add("fileName", FILENAME)
                .build();

        assertThrows(IllegalArgumentException.class, () -> alfrescoUploadService.uploadFile(command));
    }

    @Test
    public void shouldThrowExceptionIfNoFilename() throws IOException {
        JsonObject command = JsonObjects.createObjectBuilder()
                .add(MATERIAL_ID_NAME, MATERIAL_ID.toString())
                .add("document", withDocumentData())
                .build();
        assertThrows(IllegalArgumentException.class, () -> alfrescoUploadService.uploadFile(command));
    }

    private JsonObject buildRequestData() {
        return JsonObjects.createObjectBuilder()
                .add(MATERIAL_ID_NAME, MATERIAL_ID.toString())
                .add("document", withDocumentData().build())
                .add("fileName", FILENAME)
                .build();
    }

    private JsonObjectBuilder withDocumentData() {
        return JsonObjects.createObjectBuilder().add("content", DOCUMENT_DATA);
    }
}
