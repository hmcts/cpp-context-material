package uk.gov.moj.cpp.material.query.service;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.file.api.requester.FileRequester;
import uk.gov.moj.cpp.material.query.service.exception.AlfrescoReadException;
import uk.gov.moj.cpp.material.query.view.MaterialMetadataView;
import uk.gov.moj.cpp.material.query.view.MaterialView;

import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("deprecation")
@ExtendWith(MockitoExtension.class)
public class AlfrescoReadServiceTest {

    @Mock
    private MaterialService materialService;

    @Mock
    private FileRequester fileRequester;

    @InjectMocks
    private AlfrescoReadService alfrescoReadService;

    @Test
    public void shouldLookUpADocumentsMetadataThenRequestItFromAlfresco() throws Exception {

        final UUID materialId = randomUUID();
        final String alfrescoAssetId = "alfrescoAssetId";
        final String mimeType = "mimeType";
        final String fileName = "fileName";
        final InputStream documentInputStream = new StringBufferInputStream("the-document");

        final MaterialMetadataView materialMetadataView = mock(MaterialMetadataView.class);

        when(materialService.getMaterialMetadataByMaterialId(materialId)).thenReturn(materialMetadataView);
        when(materialMetadataView.getAlfrescoAssetId()).thenReturn(alfrescoAssetId);
        when(materialMetadataView.getMimeType()).thenReturn(mimeType);
        when(materialMetadataView.getFileName()).thenReturn(fileName);

        when(fileRequester.request(alfrescoAssetId, mimeType, fileName)).thenReturn(of(documentInputStream));

        final Optional<MaterialView> materialView = alfrescoReadService.getDataById(materialId);

        assertThat(materialView.get().getDocumentInputStream(), is(documentInputStream));
        assertThat(materialView.get().getFileName(), is(fileName));
        assertThat(materialView.get().getContentType(), is(mimeType));
    }

    @Test
    public void shouldReturnNulIfNoMetadataFoundForDocumentId() throws Exception {

        final UUID materialId = randomUUID();

        when(materialService.getMaterialMetadataByMaterialId(materialId)).thenReturn(null);

        assertThat(alfrescoReadService.getDataById(materialId).isPresent(), is(false));
    }

    @Test
    public void shouldThrowAnAlfrescoReadExceptionIfGettingTheMetadataThrowsException() throws Exception {

        final JsonMappingException jsonMappingException = new JsonMappingException("Ooops");

        final UUID materialId = randomUUID();

        when(materialService.getMaterialMetadataByMaterialId(materialId)).thenThrow(jsonMappingException);

        try {
            alfrescoReadService.getDataById(materialId);
            fail();
        } catch (final AlfrescoReadException expected) {
            assertThat(expected.getCause(), is(jsonMappingException));
            assertThat(expected.getMessage(), is("Json Error while fetching material for id " + materialId));
        }
    }

    @Test
    public void shouldThrowAnAlfrescoReadExceptionIfNoDocumentFoundInAlfresco() throws Exception {

        final UUID materialId = randomUUID();
        final String alfrescoAssetId = "alfrescoAssetId";
        final String mimeType = "mimeType";
        final String fileName = "fileName";

        final MaterialMetadataView materialMetadataView = mock(MaterialMetadataView.class);

        when(materialService.getMaterialMetadataByMaterialId(materialId)).thenReturn(materialMetadataView);
        when(materialMetadataView.getAlfrescoAssetId()).thenReturn(alfrescoAssetId);
        when(materialMetadataView.getMimeType()).thenReturn(mimeType);
        when(materialMetadataView.getFileName()).thenReturn(fileName);

        when(fileRequester.request(alfrescoAssetId, mimeType, fileName)).thenReturn(empty());

        try {
            alfrescoReadService.getDataById(materialId);
            fail();
        } catch (final AlfrescoReadException expected) {
            assertThat(expected.getCause(), is(nullValue()));
            final String expectedMessage = "No document found while fetching document from alfresco. " +
                    "alfrescoAssetId: 'alfrescoAssetId', " +
                    "mimeType: 'mimeType', " +
                    "fileName: 'fileName'";

            assertThat(expected.getMessage(), is(expectedMessage));
        }
    }
}
