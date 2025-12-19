package uk.gov.moj.cpp.material.event.listener;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.material.persistence.constant.StructuredFormStatus.CREATED;
import static uk.gov.moj.cpp.material.persistence.constant.StructuredFormStatus.DRAFTED;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.domain.UpdatedBy;
import uk.gov.moj.cpp.material.domain.event.StructuredFormCreated;
import uk.gov.moj.cpp.material.domain.event.StructuredFormFinalised;
import uk.gov.moj.cpp.material.domain.event.StructuredFormPublished;
import uk.gov.moj.cpp.material.domain.event.StructuredFormUpdated;
import uk.gov.moj.cpp.material.domain.event.StructuredFormUpdatedForDefendant;
import uk.gov.moj.cpp.material.event.utils.FileUtil;
import uk.gov.moj.cpp.material.persistence.constant.StructuredFormStatus;
import uk.gov.moj.cpp.material.persistence.entity.StructuredForm;
import uk.gov.moj.cpp.material.persistence.repository.StructuredFormChangeHistoryRepository;
import uk.gov.moj.cpp.material.persistence.repository.StructuredFormRepository;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StructuredFormEventListenerTest {

    @InjectMocks
    private StructuredFormEventListener structuredFormEventListener;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private StructuredFormRepository structuredFormRepository;

    @Mock
    private StructuredFormChangeHistoryRepository structuredFormChangeHistoryRepository;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter();

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldCreateStructuredForm() {

        final UUID structuredFormId = randomUUID();
        final UpdatedBy updatedBy = new UpdatedBy(randomUUID());
        final StructuredFormCreated event = StructuredFormCreated.structuredFormCreated()
                .withStructuredFormId(structuredFormId)
                .withFormId(randomUUID())
                .withStructuredFormData("Test Data")
                .withStatus(uk.gov.moj.cpp.material.domain.StructuredFormStatus.CREATED)
                .withLastUpdated(now().truncatedTo(ChronoUnit.MILLIS))
                .withUpdatedBy(updatedBy)
                .build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUID("material.events.structured-form-created"),
                objectToJsonObjectConverter.convert(event));

        structuredFormEventListener.structuredFormCreated(jsonEnvelope);

        verify(this.structuredFormRepository, times(1)).save(any());
        verify(this.structuredFormChangeHistoryRepository, times(1)).save(any());
    }

    @Test
    public void shouldUpdateStructuredForm() {

        final UUID structuredFormId = randomUUID();
        final UpdatedBy updatedBy = new UpdatedBy(randomUUID());
        final StructuredFormUpdated event = StructuredFormUpdated.structuredFormUpdated()
                .withStructuredFormId(structuredFormId)
                .withStructuredFormData("Test Data")
                .withUpdatedBy(updatedBy)
                .build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUID("material.events.structured-form-updated"),
                objectToJsonObjectConverter.convert(event));
        when(this.structuredFormRepository.findBy(structuredFormId)).thenReturn(StructuredForm.builder().withData(event.getStructuredFormData()).withId(event.getStructuredFormId()).withStatus(DRAFTED).build());
        structuredFormEventListener.structuredFormUpdated(jsonEnvelope);

        verify(this.structuredFormRepository, times(1)).save(any());
    }

    @Test
    public void shouldUpdateStructuredFormWithHistory() {

        final UUID structuredFormId = randomUUID();
        final UpdatedBy updatedBy = new UpdatedBy(randomUUID());
        final StructuredFormUpdated event = StructuredFormUpdated.structuredFormUpdated()
                .withStructuredFormId(structuredFormId)
                .withStructuredFormData("Test Data")
                .withUpdatedBy(updatedBy)
                .build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUID("material.events.structured-form-updated"),
                objectToJsonObjectConverter.convert(event));
        when(this.structuredFormRepository.findBy(structuredFormId)).thenReturn(StructuredForm.builder().withData(event.getStructuredFormData()).withId(event.getStructuredFormId()).withStatus(CREATED).build());
        structuredFormEventListener.structuredFormUpdated(jsonEnvelope);

        verify(this.structuredFormRepository, times(1)).save(any());
        verify(this.structuredFormChangeHistoryRepository, times(1)).save(any());
    }

    @Test
    public void shouldUpdateStructuredFormForDefendant() {

        final UUID structuredFormId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID witnessId = randomUUID();
        final UUID witness2Id = randomUUID();
        final String existingStructuredFormStubbedData = FileUtil.getPayload("stub-data/structured-form-sample-data.json")
                .replaceAll("DEFENDANT_ID", defendantId.toString())
                .replaceAll("WITNESS_ID", witnessId.toString())
                .replaceAll("WITNESS_2_ID", witness2Id.toString());

        final StructuredFormUpdatedForDefendant event = buildUpdateForDefendantEventPayload(structuredFormId, defendantId, witnessId, witness2Id);

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUID("material.events.structured-form-updated-for-defendant"),
                objectToJsonObjectConverter.convert(event));

        doReturn(StructuredForm.builder()
                .withId(structuredFormId)
                .withFormId(randomUUID())
                .withData(existingStructuredFormStubbedData)
                .build()).when(this.structuredFormRepository).findBy(structuredFormId);

        structuredFormEventListener.structuredFormUpdatedForDefendant(jsonEnvelope);

        verify(this.structuredFormRepository, times(1)).save(any());
    }

    @Test
    public void shouldNotUpdateStructuredFormWhenStructuredFormNotExists() {

        final UUID structuredFormId = randomUUID();
        final UpdatedBy updatedBy = new UpdatedBy(randomUUID());
        final StructuredFormUpdated event = StructuredFormUpdated.structuredFormUpdated()
                .withStructuredFormId(structuredFormId)
                .withStructuredFormData("Test Data")
                .withUpdatedBy(updatedBy)
                .build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUID("material.events.structured-form-updated"),
                objectToJsonObjectConverter.convert(event));
        structuredFormEventListener.structuredFormUpdated(jsonEnvelope);

        verify(this.structuredFormRepository, times(0)).save(any());
    }

    @Test
    public void shouldPublishStructuredForm() {

        final UUID structuredFormId = randomUUID();
        final ZonedDateTime lastUpdated = now();
        final UpdatedBy updatedBy = new UpdatedBy(randomUUID());
        final StructuredFormPublished event = StructuredFormPublished.structuredFormPublished()
                .withStructuredFormId(structuredFormId)
                .withUpdatedBy(updatedBy)
                .withLastUpdated(lastUpdated)
                .build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUID("material.events.structured-form-published"),
                objectToJsonObjectConverter.convert(event));
        final StructuredForm existingStructuredForm = createStructuredFormEntity(event, StructuredFormStatus.CREATED);
        final StructuredForm publishedStructuredForm = createStructuredFormEntity(event, StructuredFormStatus.PUBLISHED);
        when(this.structuredFormRepository.findBy(structuredFormId)).thenReturn(existingStructuredForm);
        structuredFormEventListener.structuredFormPublished(jsonEnvelope);

        verify(this.structuredFormRepository, times(1)).save(publishedStructuredForm);
        verify(this.structuredFormChangeHistoryRepository, times(1)).save(any());
    }

    @Test
    public void shouldFinaliseStructuredForm() {
        final UUID structuredFormId = randomUUID();
        final UUID materialId = randomUUID();
        final UpdatedBy updatedBy = new UpdatedBy(randomUUID());
        final StructuredFormFinalised event = StructuredFormFinalised.structuredFormFinalised()
                .withStructuredFormId(structuredFormId)
                .withMaterialId(materialId)
                .withUpdatedBy(updatedBy)
                .build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUID("material.events.structured-form-finalised"),
                objectToJsonObjectConverter.convert(event));
        final StructuredForm existingStructuredForm = createStructuredFormEntity(event, StructuredFormStatus.PUBLISHED);
        final StructuredForm expectedStructuredFormAfterFinalise = createStructuredFormEntity(event, StructuredFormStatus.FINALISED);

        when(this.structuredFormRepository.findBy(structuredFormId)).thenReturn(existingStructuredForm);
        structuredFormEventListener.structuredFormPublished(jsonEnvelope);

        verify(this.structuredFormRepository, times(1)).save(expectedStructuredFormAfterFinalise);
        verify(this.structuredFormChangeHistoryRepository, times(1)).save(any());
    }

    private StructuredForm createStructuredFormEntity(final StructuredFormPublished event, StructuredFormStatus status) {
        return StructuredForm.builder()
                .withId(event.getStructuredFormId())
                .withData(createObjectBuilder().add("data", "some data").build().toString())
                .withStatus(status)
                .withFormId(event.getStructuredFormId())
                .build();
    }

    private StructuredForm createStructuredFormEntity(final StructuredFormFinalised event, final StructuredFormStatus status) {
        return StructuredForm.builder()
                .withId(event.getStructuredFormId())
                .withData(createObjectBuilder().add("data", "some data").build().toString())
                .withStatus(status)
                .withFormId(event.getStructuredFormId())
                .build();
    }

    private StructuredFormUpdatedForDefendant buildUpdateForDefendantEventPayload(final UUID structuredFormId, final UUID defendantId, final UUID witnessId, final UUID witness2Id) {
        final UpdatedBy updatedBy = new UpdatedBy(randomUUID());
        return StructuredFormUpdatedForDefendant.structuredFormUpdatedForDefendant()
                .withStructuredFormId(structuredFormId)
                .withDefendantData(createObjectBuilder()
                        .add("defendant",
                                createObjectBuilder()
                                        .add("id", defendantId.toString())
                                        .add("defenceDynamicFormAnswers", createObjectBuilder().add("someField", "someValue").add("someOtherField", "someOtherValue").build())
                                        .add("witnesses", createArrayBuilder().add(createObjectBuilder().add("someField", "someValue").add("someOtherField", "someOtherValue").build())))
                        .add("prosecution",
                                createObjectBuilder()
                                        .add("witnesses", createArrayBuilder()
                                                .add(createObjectBuilder().add("id", witnessId.toString()).add("xExamination", createObjectBuilder().add("defendantId", defendantId.toString()).add("attendInPerson", true).add("whatIsDisputed", "Prosecution disputed text updated")))
                                                .add(createObjectBuilder().add("id", witness2Id.toString()).add("xExamination", createObjectBuilder().add("defendantId", defendantId.toString()).add("attendInPerson", false).add("whatIsDisputed", "Prosecution disputed text updated 2")))
                                        )
                        )
                        .build().toString())
                .withDefendantId(defendantId)
                .withUpdatedBy(updatedBy)
                .build();
    }

}
