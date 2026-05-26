package uk.gov.moj.cpp.material.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.file.api.remover.FileRemover;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.domain.event.MaterialDeleted;
import uk.gov.moj.cpp.material.filestore.azure.StorageFileDeleter;
import uk.gov.moj.cpp.material.filestore.azure.StoragePath;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class MaterialEventProcessorDeleteMaterialTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();
    private static final ObjectToJsonValueConverter pojoToJsonconverter = new ObjectToJsonValueConverter(OBJECT_MAPPER);

    @Mock
    private Sender sender;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private FileRemover fileRemover;

    @Mock
    private StorageFileDeleter storageFileDeleter;

    @Mock
    private Logger logger;

    @InjectMocks
    private MaterialEventProcessor materialEventProcessor;

    @Captor
    private ArgumentCaptor<Envelope<MaterialDeleted>> envelopeCaptor;

    @Test
    public void shouldHandleMaterialDeleted() throws Exception {
        final String alfrescoId = randomUUID().toString();
        final UUID materialId = randomUUID();
        final UUID fileServiceId = randomUUID();

        final MaterialDeleted materialDeleted = new MaterialDeleted(materialId, alfrescoId, fileServiceId);
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithDefaults().withName("material.events.material-deleted"),
                pojoToJsonconverter.convert(materialDeleted));

        when(jsonObjectConverter.convert(any(), any(Class.class))).thenReturn(materialDeleted);
        doNothing().when(fileRemover).remove(any());

        materialEventProcessor.materialDeleted(envelope);

        verify(sender).send(envelopeCaptor.capture());
        verify(fileRemover).remove(eq(alfrescoId));
        verify(storageFileDeleter).delete(StoragePath.internal(), fileServiceId);

        assertThat(envelopeCaptor.getValue().metadata().name(), is("public.material.material-deleted"));
        assertThat(envelopeCaptor.getValue().payload(), is(materialDeleted));
    }

    @Test
    public void shouldHandleMaterialDeletedWhenDeleteThrowsException() throws Exception {
        final String alfrescoId = randomUUID().toString();
        final UUID materialId = randomUUID();
        final UUID fileServiceId = randomUUID();

        final MaterialDeleted materialDeleted = new MaterialDeleted(materialId, alfrescoId, fileServiceId);
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithDefaults().withName("material.events.material-deleted"),
                pojoToJsonconverter.convert(materialDeleted));

        when(jsonObjectConverter.convert(any(), any(Class.class))).thenReturn(materialDeleted);
        doThrow(new RuntimeException("delete failed")).when(storageFileDeleter).delete(any(), any());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> materialEventProcessor.materialDeleted(envelope));
    }

    @Test
    public void shouldHandleMaterialDeletedWhenNoFileServiceIdProvided() throws Exception {
        final String alfrescoId = randomUUID().toString();
        final UUID materialId = randomUUID();

        final MaterialDeleted materialDeleted = new MaterialDeleted(materialId, alfrescoId, null);
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithDefaults().withName("material.events.material-deleted"),
                pojoToJsonconverter.convert(materialDeleted));

        when(jsonObjectConverter.convert(any(), any(Class.class))).thenReturn(materialDeleted);
        doNothing().when(fileRemover).remove(any());

        materialEventProcessor.materialDeleted(envelope);

        verify(sender).send(envelopeCaptor.capture());
        verify(fileRemover).remove(eq(alfrescoId));
        verifyNoInteractions(storageFileDeleter);

        assertThat(envelopeCaptor.getValue().metadata().name(), is("public.material.material-deleted"));
        assertThat(envelopeCaptor.getValue().payload(), is(materialDeleted));
    }
}
