package uk.gov.moj.cpp.material.query.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.query.view.MaterialDetailedMetadataView;
import uk.gov.moj.cpp.material.query.view.MaterialMetadataView;
import uk.gov.moj.cpp.material.query.view.MaterialQueryView;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialQueryApiTest {

    @InjectMocks
    private MaterialQueryApi materialQueryApi;

    @Mock
    private JsonEnvelope query;

    @Mock
    private MaterialQueryView materialQueryView;

    @Mock
    private Envelope<MaterialMetadataView> materialMetadataViewEnvelope;

    @Mock
    private Envelope<MaterialDetailedMetadataView> materialDetailedMetadataViewEnvelope;

    @Mock
    private JsonEnvelope result;

    @Test
    public void shouldReturnMaterialMetadata() throws Exception {
        when(materialQueryView.findMaterialMetadata(query)).thenReturn(materialMetadataViewEnvelope);

        assertThat(materialQueryApi.findMaterialMetadata(query), equalTo(materialMetadataViewEnvelope));
    }

    @Test
    public void shouldReturnMaterialDetailMetadata() throws Exception {
        when(materialQueryView.findMaterialMetadataDetails(query)).thenReturn(materialDetailedMetadataViewEnvelope);

        assertThat(materialQueryApi.findMaterialMetadataDetails(query), equalTo(materialDetailedMetadataViewEnvelope));
    }

    @Test
    public void shouldReturnMaterialsIsDownloadableStatus() throws Exception {
        when(materialQueryView.findDownloadableMaterials(query)).thenReturn(result);

        assertThat(materialQueryApi.findDownloadableMaterials(query), equalTo(result));
    }

}