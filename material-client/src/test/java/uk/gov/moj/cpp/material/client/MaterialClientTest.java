package uk.gov.moj.cpp.material.client;


import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.moj.cpp.material.MaterialUrls.MATERIAL_METADATA_REQUEST_PATH;
import static uk.gov.moj.cpp.material.client.MaterialClient.REQUEST_PARAM_ADD_INLINE_CONTENT_DISPOSITION_HEADER;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import javax.json.Json;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialClientTest {

    private static final String BASE_URI = "http://localhost:8080/material-query-api/query/api/rest/material";
    private static final String COMMAND_BASE_URI = "http://localhost:8080/material-command-api/command/api/rest/material";
    private static final String QUERY_MATERIAL_METADATA_DETAILS = "application/vnd.material.query.material-metadata-details+json";
    private static final String MATERIAL_ID_VALUE = randomUUID().toString();
    private static final String USER_ID_VALUE = randomUUID().toString();
    private static final String REQUEST_PARAM_STREAM = "stream";
    private static final String REQUEST_PARAM_REQUEST_PDF = "requestPdf";
    private static final String GET_MATERIAL_AS_PDF = "application/vnd.material.query.material+json";
    private static final String REMOVE_MATERIAL = "application/vnd.material.command.delete-material+json";
    private static final String CREATE_BUNDLE_MATERIAL = "application/vnd.material.command.create-material-bundle+json";
    public static final String SOURCE = "source";
    public static final String CORRESPONDENCE_ID = "correspondenceId";

    private final Client client = mock(Client.class);
    final MaterialClient materialClient = new MaterialClient() {
        @Override
        Client getClient() {
            return client;
        }
    };
    private final WebTarget webTarget = mock(WebTarget.class);
    private final Invocation.Builder builder = mock(Invocation.Builder.class);
    private final Response response = mock(Response.class);

    @BeforeEach
    public void init() {
        when(client.target(anyString())).thenReturn(webTarget);
        when(webTarget.path(anyString())).thenReturn(webTarget);
        when(webTarget.queryParam(anyString(), any())).thenReturn(webTarget);
        when(webTarget.queryParam(anyString(), any())).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(builder);
        when(builder.header(anyString(), anyString())).thenReturn(builder);
        when(builder.accept(anyString())).thenReturn(builder);
        when(builder.get()).thenReturn(response);
    }

    @Test
    public void testGetMaterialAsPdf() {
        materialClient.getMaterialAsPdf(MATERIAL_ID_VALUE, USER_ID_VALUE);
        verifyRequest(false, true, true);
    }

    @Test
    public void testGetMetadataDetails() {
        materialClient.getMetadataDetails(MATERIAL_ID_VALUE, USER_ID_VALUE);
        verify(client).target(BASE_URI);
        verify(webTarget).path(format(MATERIAL_METADATA_REQUEST_PATH,MATERIAL_ID_VALUE));
        verify(webTarget).request();
        verify(builder).header(eq(USER_ID), anyString());
        verify(builder).accept(QUERY_MATERIAL_METADATA_DETAILS);
        verify(builder).get();
    }

    @Test
    public void testGetMaterial() {
        materialClient.getMaterial(UUID.fromString(MATERIAL_ID_VALUE), UUID.fromString(USER_ID_VALUE));
        verifyRequest(true, false, false);
    }

    @Test
    public void testGetMaterialAsPdfAttachment() {
        materialClient.getMaterialAsPdfAttachment(MATERIAL_ID_VALUE, USER_ID_VALUE);
        verifyRequest(false, true, false);
    }

    @Test
    public void testGetMaterialWithHeader() {
        materialClient.getMaterialWithHeader(UUID.fromString(MATERIAL_ID_VALUE), UUID.fromString(USER_ID_VALUE));
        verifyRequest(true, false, true);
    }

    @Test
    public void testRemoveMaterial() {
        materialClient.removeMaterial(UUID.fromString(MATERIAL_ID_VALUE), UUID.fromString(USER_ID_VALUE), null);
        verifyRequest(REMOVE_MATERIAL, null);
    }

    @Test
    public void shouldCallCreateMaterialBundleWithoutAdditionalMetadata() {
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithDefaults().withName("material.material-added"),
                Json.createObjectBuilder()
                        .add(SOURCE, "aa")
                        .add(CORRESPONDENCE_ID, UUID.randomUUID().toString())
                        .add(ID, randomUUID().toString())
                        .add("caseId", UUID.randomUUID().toString()).build()
        );

        materialClient.createMaterialBundle(randomUUID(), "test.pdf", asList(fromString("06982628-e8d7-41e8-ada4-379ce07a7e86"), fromString("0e6bb9c1-9e62-4836-a1a3-8cc56d864c75")), USER_ID_VALUE, null, envelope.metadata());
        verifyRequest(CREATE_BUNDLE_MATERIAL, null);
    }

    @Test
    public void shouldCallCreateMaterialBundleWithAdditionalMetadata() {
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithDefaults().withName("material.material-added"),
                Json.createObjectBuilder()
                        .add(SOURCE, "aa")
                        .add(CORRESPONDENCE_ID, UUID.randomUUID().toString())
                        .add(ID, randomUUID().toString())
                        .add("caseId", UUID.randomUUID().toString()).build()
        );
        final Map<String, String> headerParameters = Collections.singletonMap("key1", "value1");
        materialClient.createMaterialBundle(randomUUID(), "test.pdf", asList(fromString("06982628-e8d7-41e8-ada4-379ce07a7e86"), fromString("0e6bb9c1-9e62-4836-a1a3-8cc56d864c75")), USER_ID_VALUE, headerParameters, envelope.metadata());
        verifyRequest(CREATE_BUNDLE_MATERIAL, headerParameters);
    }

    private void verifyRequest(final boolean asStream, final boolean asPdf, final boolean addInlineContentDispositionHeader) {
        verify(client).target(BASE_URI);
        verify(webTarget).path(anyString());
        verify(webTarget).queryParam(REQUEST_PARAM_STREAM, asStream);
        verify(webTarget).queryParam(REQUEST_PARAM_REQUEST_PDF, asPdf);
        verify(webTarget).queryParam(REQUEST_PARAM_ADD_INLINE_CONTENT_DISPOSITION_HEADER, addInlineContentDispositionHeader);
        verify(webTarget).request();
        verify(builder).header(eq(USER_ID), anyString());
        verify(builder).accept(GET_MATERIAL_AS_PDF);
        verify(builder).get();
    }

    private void verifyRequest(final String accept, final Map<String, String> headerParameters) {
        verify(client).target(COMMAND_BASE_URI);
        verify(webTarget).path(anyString());
        verify(webTarget).request();
        verify(builder).header(eq(USER_ID), anyString());
        verify(builder).accept(accept);
        verify(builder).post(any(Entity.class));
    }
}
