package uk.gov.moj.cpp.material.command.api;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.material.command.services.DownloadableMaterialsService;

import java.util.HashMap;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DownloadableMaterialCommandApiTest {

    @Mock
    DownloadableMaterialsService downloadableMaterialsService;

    @InjectMocks
    DownloadableMaterialCommandApi downloadableMaterialCommandApi;

    @Test
    public void shouldFindMaterialsIsDownloadableStatus() {
        final UUID MaterialId1 = randomUUID();
        final UUID MaterialId2 = randomUUID();

        final JsonEnvelope command = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithDefaults().withName("any-name"),
                Json.createObjectBuilder()
                        .add("materialIds", Json.createArrayBuilder().add(MaterialId1.toString()).add(MaterialId2.toString()).build())
                        .build()
        );
        final HashMap<UUID, Boolean> statusMap = new HashMap<>();
        statusMap.put(MaterialId1, true);
        statusMap.put(MaterialId2, false);

        when(downloadableMaterialsService.getDownloadableMaterials(any())).thenReturn(statusMap);

        Envelope envelope = downloadableMaterialCommandApi.publishDownloadableMaterials(command);

        final JsonObject resultPayload = (JsonObject)envelope.payload();

        assertThat(resultPayload.getJsonObject("materials").getString(MaterialId1.toString()), is("true"));
        assertThat(resultPayload.getJsonObject("materials").getString(MaterialId2.toString()), is("false"));
    }
}
