package uk.gov.moj.cpp.material.client;

import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * J17/J25 parity characterisation (J17 golden master), MA-M03 / BC-11.
 *
 * createMaterialBundle builds the create-material-bundle body with the framework JsonObjects builders on J17; the J25
 * upgrade switches to the raw JsonObjects.getJsonBuilderFactory() builders (Parsson), which do not go through the
 * framework helper's own null check. Pin the exact body, and what happens when a null bundle name or a null header
 * parameter value reaches the builder.
 */
class MaterialClientBundleParityTest {

    private static final UUID BUNDLE_ID = fromString("6f0e2b5a-8a7c-4c1d-9b0e-2f1d3c4b5a69");
    private static final List<UUID> MATERIAL_IDS = asList(fromString("06982628-e8d7-41e8-ada4-379ce07a7e86"), fromString("0e6bb9c1-9e62-4836-a1a3-8cc56d864c75"));
    private static final String USER_ID = "2b0f6f6e-3c7a-4f3e-9d0e-6d2b8a1c4e51";
    private static final Metadata METADATA = JsonEnvelope.metadataBuilder()
            .withId(fromString("9e8d7c6b-5a49-4382-9170-6f5e4d3c2b1a"))
            .withName("material.material-added")
            .build();

    private final Client client = mock(Client.class);
    private final WebTarget webTarget = mock(WebTarget.class);
    private final Invocation.Builder builder = mock(Invocation.Builder.class);

    private final MaterialClient materialClient = new MaterialClient() {
        @Override
        Client getClient() {
            return client;
        }
    };

    @BeforeEach
    void mockClient() {
        when(client.target(anyString())).thenReturn(webTarget);
        when(webTarget.path(anyString())).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(builder);
        when(builder.header(anyString(), any())).thenReturn(builder);
        when(builder.accept(anyString())).thenReturn(builder);
    }

    @Test
    @SuppressWarnings("unchecked")
    void maM03_shouldPostTheJ17CreateMaterialBundleBody() {
        final Map<String, String> headerParameters = new LinkedHashMap<>();
        headerParameters.put("source", "café \"quoted\"");
        headerParameters.put("caseId", "7c1d2e3f-4a5b-4c6d-8e9f-0a1b2c3d4e5f");

        materialClient.createMaterialBundle(BUNDLE_ID, "bundle.pdf", MATERIAL_IDS, USER_ID, headerParameters, METADATA);

        final ArgumentCaptor<Entity<Object>> entity = ArgumentCaptor.forClass(Entity.class);
        verify(builder).post(entity.capture());
        assertThat(entity.getValue().getEntity().toString(), is("{\"bundledMaterialId\":\"6f0e2b5a-8a7c-4c1d-9b0e-2f1d3c4b5a69\",\"bundledMaterialName\":\"bundle.pdf\","
                + "\"materialIds\":[\"06982628-e8d7-41e8-ada4-379ce07a7e86\",\"0e6bb9c1-9e62-4836-a1a3-8cc56d864c75\"],"
                + "\"_metadata\":{\"name\":\"material.command.create-material-bundle\",\"id\":\"9e8d7c6b-5a49-4382-9170-6f5e4d3c2b1a\","
                + "\"context\":{\"user\":\"2b0f6f6e-3c7a-4f3e-9d0e-6d2b8a1c4e51\"},\"source\":\"café \\\"quoted\\\"\","
                + "\"caseId\":\"7c1d2e3f-4a5b-4c6d-8e9f-0a1b2c3d4e5f\"}}"));
    }

    @Test
    void maM03_nullBundleNameShouldFailAsOnJ17() {
        final Exception exception = assertThrows(Exception.class,
                () -> materialClient.createMaterialBundle(BUNDLE_ID, null, MATERIAL_IDS, USER_ID, null, METADATA));

        assertThat(exception.getClass().getName() + ": " + exception.getMessage(), is("java.lang.NullPointerException: Value in JsonObjects name/value pair cannot be null"));
    }

    @Test
    void maM03_nullHeaderParameterValueShouldFailAsOnJ17() {
        final Map<String, String> headerParameters = new LinkedHashMap<>();
        headerParameters.put("source", null);

        final Exception exception = assertThrows(Exception.class,
                () -> materialClient.createMaterialBundle(BUNDLE_ID, "bundle.pdf", MATERIAL_IDS, USER_ID, headerParameters, METADATA));

        assertThat(exception.getClass().getName() + ": " + exception.getMessage(), is("java.lang.NullPointerException: Value in JsonObjects name/value pair cannot be null"));
    }
}
