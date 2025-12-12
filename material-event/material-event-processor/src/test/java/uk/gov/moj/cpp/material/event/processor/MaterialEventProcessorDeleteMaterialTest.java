package uk.gov.moj.cpp.material.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.file.api.remover.FileRemover;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.domain.event.MaterialDeleted;

import java.util.Optional;
import java.util.UUID;

import javax.json.Json;

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
    private FileService fileService;

    @Mock
    private FileReference fileReferenceData;

    @Mock
    private Logger logger;

    @InjectMocks
    private MaterialEventProcessor materialEventProcessor;

    @Captor
    private ArgumentCaptor<Envelope<MaterialDeleted>> envelopeCaptor;

    @Test
    public void shouldHandleMaterialDeleted() throws Exception {
        final String ALFRESCO_ID = randomUUID().toString();
        final UUID MATERIAL_ID = randomUUID();
        final UUID FILE_SERVICE_ID = randomUUID();

        final MaterialDeleted materialDeleted = new MaterialDeleted(MATERIAL_ID, ALFRESCO_ID, FILE_SERVICE_ID);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithDefaults().withName("material.events.material-deleted"),
                pojoToJsonconverter.convert(materialDeleted)
        );

        when(jsonObjectConverter.convert(any(), any(Class.class))).thenReturn(materialDeleted);
        doNothing().when(fileRemover).remove(any());
        FileReference fileReference = new FileReference(randomUUID(), Json.createObjectBuilder().build(), null);
        when(fileService.retrieve(any())).thenReturn(Optional.of(fileReference));
        doNothing().when(fileService).delete(any());

        materialEventProcessor.materialDeleted(envelope);

        verify(sender).send(envelopeCaptor.capture());
        verify(fileRemover).remove(eq(ALFRESCO_ID));
        verify(fileService).retrieve(eq(FILE_SERVICE_ID));
        verify(fileService).delete(eq(FILE_SERVICE_ID));

        assertThat(envelopeCaptor.getValue().metadata().name(), is("public.material.material-deleted"));
        assertThat(envelopeCaptor.getValue().payload(), is(materialDeleted));

    }

    @Test
    public void shouldHandleMaterialDeletedEvenWhenFileServiceReturnEmpty() throws Exception {
        final String ALFRESCO_ID = randomUUID().toString();
        final UUID MATERIAL_ID = randomUUID();
        final UUID FILE_SERVICE_ID = randomUUID();


        final MaterialDeleted materialDeleted = new MaterialDeleted(MATERIAL_ID, ALFRESCO_ID, FILE_SERVICE_ID);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithDefaults().withName("material.events.material-deleted"),
                pojoToJsonconverter.convert(materialDeleted)
        );

        when(jsonObjectConverter.convert(any(), any(Class.class))).thenReturn(materialDeleted);
        doNothing().when(fileRemover).remove(any());
        when(fileService.retrieve(any())).thenReturn(Optional.empty());

        materialEventProcessor.materialDeleted(envelope);

        verify(sender).send(envelopeCaptor.capture());
        verify(fileRemover).remove(eq(ALFRESCO_ID));
        verify(fileService).retrieve(eq(FILE_SERVICE_ID));
        verify(fileService, times(0)).delete(eq(FILE_SERVICE_ID));

        assertThat(envelopeCaptor.getValue().metadata().name(), is("public.material.material-deleted"));
        assertThat(envelopeCaptor.getValue().payload(), is(materialDeleted));
    }


    @Test
    public void shouldHandleMaterialDeletedEvenWhenFileServiceRetriveThrowAnException() throws Exception {
        final String ALFRESCO_ID = randomUUID().toString();
        final UUID MATERIAL_ID = randomUUID();
        final UUID FILE_SERVICE_ID = randomUUID();


        final MaterialDeleted materialDeleted = new MaterialDeleted(MATERIAL_ID, ALFRESCO_ID, FILE_SERVICE_ID);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithDefaults().withName("material.events.material-deleted"),
                pojoToJsonconverter.convert(materialDeleted)
        );

        when(jsonObjectConverter.convert(any(), any(Class.class))).thenReturn(materialDeleted);
        when(fileService.retrieve(any())).thenThrow(new FileServiceException("Can not get File"));

        assertThrows(FileServiceException.class, () -> materialEventProcessor.materialDeleted(envelope));
    }

    @Test
    public void shouldHandleMaterialDeletedEvenWhenFileServiceCloseThrowAnException() throws Exception {
        final String ALFRESCO_ID = randomUUID().toString();
        final UUID MATERIAL_ID = randomUUID();
        final UUID FILE_SERVICE_ID = randomUUID();


        final MaterialDeleted materialDeleted = new MaterialDeleted(MATERIAL_ID, ALFRESCO_ID, FILE_SERVICE_ID);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithDefaults().withName("material.events.material-deleted"),
                pojoToJsonconverter.convert(materialDeleted)
        );

        when(jsonObjectConverter.convert(any(), any(Class.class))).thenReturn(materialDeleted);
        when(fileService.retrieve(any())).thenReturn(Optional.of(fileReferenceData));
        doThrow(new Exception()).when(fileReferenceData).close();

        assertThrows(Exception.class, () -> materialEventProcessor.materialDeleted(envelope));
    }

    @Test
    public void shouldHandleMaterialDeletedEvenDeleteThrowAnError() throws Exception {
        final String ALFRESCO_ID = randomUUID().toString();
        final UUID MATERIAL_ID = randomUUID();
        final UUID FILE_SERVICE_ID = randomUUID();

        final MaterialDeleted materialDeleted = new MaterialDeleted(MATERIAL_ID, ALFRESCO_ID, FILE_SERVICE_ID);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithDefaults().withName("material.events.material-deleted"),
                pojoToJsonconverter.convert(materialDeleted)
        );

        when(jsonObjectConverter.convert(any(), any(Class.class))).thenReturn(materialDeleted);
        FileReference fileReference = new FileReference(randomUUID(), Json.createObjectBuilder().build(), null);
        when(fileService.retrieve(any())).thenReturn(Optional.of(fileReference));
        doThrow(new FileServiceException("Can not be deleted")).when(fileService).delete(any());

        assertThrows(FileServiceException.class, () -> materialEventProcessor.materialDeleted(envelope));
    }


    @Test
    public void shouldHandleMaterialDeletedWhenNoFlieServiceIdProvided() throws Exception {
        final String ALFRESCO_ID = randomUUID().toString();
        final UUID MATERIAL_ID = randomUUID();

        final MaterialDeleted materialDeleted = new MaterialDeleted(MATERIAL_ID, ALFRESCO_ID, null);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithDefaults().withName("material.events.material-deleted"),
                pojoToJsonconverter.convert(materialDeleted)
        );

        when(jsonObjectConverter.convert(any(), any(Class.class))).thenReturn(materialDeleted);
        doNothing().when(fileRemover).remove(any());

        materialEventProcessor.materialDeleted(envelope);

        verify(sender).send(envelopeCaptor.capture());
        verify(fileRemover).remove(eq(ALFRESCO_ID));
        verifyNoInteractions(fileService);

        assertThat(envelopeCaptor.getValue().metadata().name(), is("public.material.material-deleted"));
        assertThat(envelopeCaptor.getValue().payload(), is(materialDeleted));
    }
}