package uk.gov.moj.cpp.material.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.domain.FileDetails;
import uk.gov.moj.cpp.material.domain.event.FailedToAddMaterial;
import uk.gov.moj.cpp.material.domain.event.FileUploaded;
import uk.gov.moj.cpp.material.domain.event.MaterialAdded;
import uk.gov.moj.cpp.material.domain.event.MaterialBundleDetailsRecorded;
import uk.gov.moj.cpp.material.domain.event.MaterialDeleted;
import uk.gov.moj.cpp.material.event.listener.converter.MaterialAddedToMaterialConverter;
import uk.gov.moj.cpp.material.event.listener.converter.MaterialBundleDetailsRecordedToMaterialConverter;
import uk.gov.moj.cpp.material.persistence.entity.Material;
import uk.gov.moj.cpp.material.persistence.entity.MaterialUploadStatus;
import uk.gov.moj.cpp.material.persistence.repository.MaterialRepository;
import uk.gov.moj.cpp.material.persistence.repository.MaterialUploadStatusRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class MaterialEventListenerTest {

    private static final UUID MATERIAL_ID = randomUUID();
    private static final UUID FILE_SERVICE_ID = randomUUID();
    private static final String ALFRESCO_ID = "jhdfjksdhfdjksfgsdkg534534563";
    private static final String FILENAME = "test.txt";
    private static final String MIME_TYPE = "text/plain";
    private static final ZonedDateTime MATERIAL_ADDED_DATE = new UtcClock().now();
    private static final ZonedDateTime TIME_NOW = new UtcClock().now();
    private static final ZonedDateTime FAILED_TIME = TIME_NOW.minusSeconds(2);
    private static final String EXTERNAL_LINK = "http://something.com";

    @Spy
    private MaterialAddedToMaterialConverter materialAddedToMaterialConverter = new MaterialAddedToMaterialConverter();

    @InjectMocks
    private MaterialEventListener materialEventListener;

    @Mock
    private JsonEnvelope materialAddedEnvelope;

    @Mock
    private JsonEnvelope fileUploadedEnvelope;

    @Mock
    private JsonEnvelope failedToUploadMaterialEnvelope;

    @Mock
    private JsonObject materialAddedAsJsonObject;

    @Mock
    private JsonObject fileUploadedAsJsonObject;

    @Mock
    private JsonObject failedToUploadMaterialAsJsonObject;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private MaterialUploadStatusRepository materialUploadStatusRepository;

    @Mock
    private MaterialUploadStatus materialUploadStatus;

    @Mock
    private UtcClock clock;

    @Mock
    private Logger logger;

    @Mock
    private Envelope<MaterialBundleDetailsRecorded> envelope;

    @Captor
    private ArgumentCaptor<MaterialUploadStatus> materialUploadStatusArgumentCaptor;

    @Test
    public void shouldHandleMaterialAddedEvent_WhenUploadDetailsProvided_And_No_ExternalLink_AndMarkUploadStatusSuccess() {
        final MaterialAdded materialAdded = new MaterialAdded(MATERIAL_ID, new FileDetails(ALFRESCO_ID, MIME_TYPE, FILENAME), MATERIAL_ADDED_DATE, false);
        final Material material = new Material(MATERIAL_ID, ALFRESCO_ID, FILENAME, MIME_TYPE, MATERIAL_ADDED_DATE, null);
        when(materialAddedEnvelope.payloadAsJsonObject()).thenReturn(materialAddedAsJsonObject);
        when(jsonObjectConverter.convert(materialAddedAsJsonObject, MaterialAdded.class)).thenReturn(materialAdded);
        when(materialUploadStatusRepository.findBy(MATERIAL_ID)).thenReturn(materialUploadStatus);
        when(clock.now()).thenReturn(TIME_NOW);

        materialEventListener.materialAdded(this.materialAddedEnvelope);

        verify(materialRepository).save(material);
        verify(materialAddedEnvelope).payloadAsJsonObject();
        verify(jsonObjectConverter).convert(eq(materialAddedAsJsonObject), eq(MaterialAdded.class));

        verify(materialUploadStatus).setLastModified(TIME_NOW);
        verify(materialUploadStatus).setStatus("SUCCESS");
        verify(materialUploadStatusRepository).save(materialUploadStatus);
    }

    @Test
    public void shouldHandleMaterialAddedEvent_WhenUploadDetailsProvided_And_No_ExternalLink_AndWarnUpdateStatusFailedWhenNotFound() {
        final MaterialAdded materialAdded = new MaterialAdded(MATERIAL_ID, new FileDetails(ALFRESCO_ID, MIME_TYPE, FILENAME), MATERIAL_ADDED_DATE, false);
        final Material material = new Material(MATERIAL_ID, ALFRESCO_ID, FILENAME, MIME_TYPE, MATERIAL_ADDED_DATE, null);
        when(materialAddedEnvelope.payloadAsJsonObject()).thenReturn(materialAddedAsJsonObject);
        when(jsonObjectConverter.convert(materialAddedAsJsonObject, MaterialAdded.class)).thenReturn(materialAdded);
        when(materialUploadStatusRepository.findBy(MATERIAL_ID)).thenReturn(null);

        materialEventListener.materialAdded(this.materialAddedEnvelope);

        verify(materialRepository).save(material);
        verify(materialAddedEnvelope).payloadAsJsonObject();
        verify(jsonObjectConverter).convert(eq(materialAddedAsJsonObject), eq(MaterialAdded.class));

        verifyNoMoreInteractions(materialUploadStatus);
        verify(materialUploadStatusRepository, never()).save(any());

        verify(logger).warn("Failed to upload status of materialId {} to {} as it was not found.", MATERIAL_ID, "SUCCESS");
    }

    @Test
    public void shouldHandleMaterialAddedEvent_WhenExternalLinkProvided_And_No_UploadDetails() {
        final MaterialAdded materialAdded = new MaterialAdded(MATERIAL_ID, new FileDetails(EXTERNAL_LINK, FILENAME), MATERIAL_ADDED_DATE, false);
        final Material material = new Material(MATERIAL_ID, null, FILENAME, null, MATERIAL_ADDED_DATE, EXTERNAL_LINK);
        when(materialAddedEnvelope.payloadAsJsonObject()).thenReturn(materialAddedAsJsonObject);
        when(jsonObjectConverter.convert(materialAddedAsJsonObject, MaterialAdded.class)).thenReturn(materialAdded);

        materialEventListener.materialAdded(this.materialAddedEnvelope);

        verify(materialRepository).save(material);
        verify(materialAddedEnvelope).payloadAsJsonObject();
        verify(jsonObjectConverter).convert(eq(materialAddedAsJsonObject), eq(MaterialAdded.class));
    }

    @Test
    public void shouldHandleMaterialDeletedEvent() {
        final JsonObject materialDeletedAsJsonObject = mock(JsonObject.class);
        final JsonEnvelope materialDeletedEnvelope = mock(JsonEnvelope.class);
        final MaterialDeleted materialDeleted = new MaterialDeleted(MATERIAL_ID, randomUUID().toString(), randomUUID());
        final Material material = new Material(MATERIAL_ID, ALFRESCO_ID, FILENAME, MIME_TYPE, MATERIAL_ADDED_DATE, null);
        when(materialDeletedEnvelope.payloadAsJsonObject()).thenReturn(materialDeletedAsJsonObject);
        when(jsonObjectConverter.convert(materialDeletedAsJsonObject, MaterialDeleted.class)).thenReturn(materialDeleted);
        when(materialRepository.findBy(materialDeleted.getMaterialId())).thenReturn(material);

        materialEventListener.materialDeleted(materialDeletedEnvelope);

        verify(materialRepository).removeAndFlush(material);
        verify(materialDeletedEnvelope).payloadAsJsonObject();
        verify(jsonObjectConverter).convert(eq(materialDeletedAsJsonObject), eq(MaterialDeleted.class));
    }

    @Test
    public void shouldSetMaterialUpdateStatusAsQueuedWhenFileUploadRequestReceived() {
        FileUploaded fileUploaded = new FileUploaded(MATERIAL_ID, FILE_SERVICE_ID, false);
        when(fileUploadedEnvelope.payloadAsJsonObject()).thenReturn(fileUploadedAsJsonObject);
        when(jsonObjectConverter.convert(fileUploadedAsJsonObject, FileUploaded.class)).thenReturn(fileUploaded);
        when(materialUploadStatusRepository.findBy(MATERIAL_ID)).thenReturn(null);
        when(clock.now()).thenReturn(TIME_NOW);

        materialEventListener.fileUploaded(this.fileUploadedEnvelope);

        verify(materialUploadStatusRepository).save(materialUploadStatusArgumentCaptor.capture());

        final MaterialUploadStatus savedEntity = materialUploadStatusArgumentCaptor.getValue();
        assertThat(savedEntity.getMaterialId(), is(MATERIAL_ID));
        assertThat(savedEntity.getFileServiceId(), is(FILE_SERVICE_ID));
        assertThat(savedEntity.getStatus(), is("QUEUED"));
        assertThat(savedEntity.getLastModified(), is(TIME_NOW));
        assertThat(savedEntity.getErrorMessage(), is(nullValue()));
        assertThat(savedEntity.getFailedTime(), is(nullValue()));
    }

    @Test
    public void shouldSetLogWarningForMaterialUpdateStatusWhenExistingRecordFound() {
        FileUploaded fileUploaded = new FileUploaded(MATERIAL_ID, FILE_SERVICE_ID, false);
        when(fileUploadedEnvelope.payloadAsJsonObject()).thenReturn(fileUploadedAsJsonObject);
        when(jsonObjectConverter.convert(fileUploadedAsJsonObject, FileUploaded.class)).thenReturn(fileUploaded);
        when(materialUploadStatusRepository.findBy(MATERIAL_ID)).thenReturn(materialUploadStatus);

        materialEventListener.fileUploaded(this.fileUploadedEnvelope);

        verifyNoMoreInteractions(materialUploadStatus);
        verify(materialUploadStatusRepository, never()).save(any());

        verify(logger).warn("Failed to add new upload status for materialId {} as it already exists.", MATERIAL_ID);
    }

    @Test
    public void shouldSetMaterialUpdateStatusAsFailedWhenUploadToAlfresoFailed() {
        FailedToAddMaterial failedToAddMaterial = new FailedToAddMaterial(MATERIAL_ID, FILE_SERVICE_ID, FAILED_TIME, "it broke");
        when(failedToUploadMaterialEnvelope.payloadAsJsonObject()).thenReturn(failedToUploadMaterialAsJsonObject);
        when(jsonObjectConverter.convert(failedToUploadMaterialAsJsonObject, FailedToAddMaterial.class)).thenReturn(failedToAddMaterial);
        when(materialUploadStatusRepository.findBy(MATERIAL_ID)).thenReturn(materialUploadStatus);
        when(clock.now()).thenReturn(TIME_NOW);

        materialEventListener.failedToAddMaterial(this.failedToUploadMaterialEnvelope);

        verify(materialUploadStatusRepository).save(materialUploadStatusArgumentCaptor.capture());

        verify(materialUploadStatus).setLastModified(TIME_NOW);
        verify(materialUploadStatus).setStatus("FAILED");
        verify(materialUploadStatus).setFailedTime(FAILED_TIME);
        verify(materialUploadStatus).setErrorMessage("it broke");
        verify(materialUploadStatusRepository).save(materialUploadStatus);
    }

    @Test
    public void shouldSetLogWarningForMaterialUpdateStatusWhenExistingRecordNotFound() {
        FailedToAddMaterial failedToAddMaterial = new FailedToAddMaterial(MATERIAL_ID, FILE_SERVICE_ID, FAILED_TIME, "it broke");
        when(failedToUploadMaterialEnvelope.payloadAsJsonObject()).thenReturn(failedToUploadMaterialAsJsonObject);
        when(jsonObjectConverter.convert(failedToUploadMaterialAsJsonObject, FailedToAddMaterial.class)).thenReturn(failedToAddMaterial);
        when(materialUploadStatusRepository.findBy(MATERIAL_ID)).thenReturn(null);

        materialEventListener.failedToAddMaterial(this.failedToUploadMaterialEnvelope);

        verifyNoMoreInteractions(materialUploadStatus);
        verify(materialUploadStatusRepository, never()).save(any());

        verify(logger).warn("Failed to upload status of materialId {} to {} as it was not found.", MATERIAL_ID, "FAILED");
    }

    @Test
    public void shouldHandleMaterialBundleDetailsRecordedEvent_WhenMaterialBundleDetailsRecorded() {
        materialEventListener.materialBundleDetailsRecordedToMaterialConverter = new MaterialBundleDetailsRecordedToMaterialConverter();
        final MaterialBundleDetailsRecorded materialBundleDetailsRecorded = new MaterialBundleDetailsRecorded(MATERIAL_ID, new FileDetails(ALFRESCO_ID, MIME_TYPE, FILENAME),"1", 1, MATERIAL_ADDED_DATE);
        final Material material = new Material(MATERIAL_ID, ALFRESCO_ID, FILENAME, MIME_TYPE, MATERIAL_ADDED_DATE, null);
        when(envelope.payload()).thenReturn(materialBundleDetailsRecorded);
        materialEventListener.materialBundleDetailsRecorded(this.envelope);
        verify(envelope).payload();
        verify(materialRepository).save(material);
    }
}
