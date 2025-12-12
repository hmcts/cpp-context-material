package uk.gov.moj.cpp.material.command.handler.alfresco;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.OK;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.material.command.handler.alfresco.exception.AlfrescoUploadException;
import uk.gov.moj.cpp.material.command.handler.client.RestClient;
import uk.gov.moj.cpp.material.domain.UploadedMaterial;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;

/**
 * Service to upload material to Alfresco.
 */
public class AlfrescoUploadService {

    private static final String FORM_FILED_FILEDATA = "filedata";
    private static final String CPPUID = "cppuid";

    @Inject
    @Value(key = "alfrescoBaseUri")
    String alfrescoBaseUri;

    @Inject
    @Value(key = "alfrescoUploadPath", defaultValue = "/service/case/upload")
    String alfrescoUploadPath;

    @Inject
    @Value(key = "alfrescoUploadUser")
    String alfrescoUploadUser;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private RestClient restClient;

    /**
     * Uploads the document from addMaterialCommand to Alfresco.
     *
     * @param addMaterialCommand command containing the document and fileName.
     * @return alfrescoAssetId
     */
    public UploadedMaterial uploadFile(final JsonObject addMaterialCommand) throws IOException {

        final String document = JsonObjects.getString(addMaterialCommand, "document", "content")
                .orElseThrow(() -> new IllegalArgumentException("document is required."));

        final String fileName = JsonObjects.getString(addMaterialCommand, "fileName")
                .orElseThrow(() -> new IllegalArgumentException("fileName is required."));

        return uploadToAlfresco(fileName, document);
    }

    private UploadedMaterial uploadToAlfresco(final String fileName, final String document)
            throws IOException {

        final Response response = upload(fileName, document);

        final String responseEntity = response.readEntity(String.class);

        if (response.getStatus() != OK.getStatusCode() || responseEntity == null) {
            //Alfresco is *very* accepting - failed exceptions represent service outages/problems only.
            throw new AlfrescoUploadException(String.format("Error while uploading document. Code:%d, Reason:%s", response.getStatus(), response.getStatusInfo().getReasonPhrase()));
        }

        final AlfrescoUploadResponse alfrescoUploadResponse = objectMapper.readValue(responseEntity, AlfrescoUploadResponse.class);
        final String alfrescoAssetId = alfrescoUploadResponse.getNodeRef().replace("workspace://SpacesStore/", "");

        return new UploadedMaterial(alfrescoAssetId, alfrescoUploadResponse.getFileMimeType());
    }

    private Response upload(final String fileName, final String document) {
        final byte[] decodedDocument = Base64.getDecoder().decode(document);

        final MultipartFormDataOutput multipartFormDataOutput = new MultipartFormDataOutput();
        multipartFormDataOutput.addFormData(FORM_FILED_FILEDATA, decodedDocument, TEXT_PLAIN_TYPE, fileName);

        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.put(CPPUID, Collections.singletonList(alfrescoUploadUser));

        final Entity<MultipartFormDataOutput> entity = Entity.entity(multipartFormDataOutput, MULTIPART_FORM_DATA_TYPE);

        return restClient.post(alfrescoBaseUri + alfrescoUploadPath, MULTIPART_FORM_DATA_TYPE, headers, entity);
    }

}
