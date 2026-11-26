package uk.gov.moj.cpp.material.event.listener;

import static java.util.UUID.randomUUID;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.domain.event.StructuredFormCreated;
import uk.gov.moj.cpp.material.domain.event.StructuredFormFinalised;
import uk.gov.moj.cpp.material.domain.event.StructuredFormPublished;
import uk.gov.moj.cpp.material.domain.event.StructuredFormUpdated;
import uk.gov.moj.cpp.material.domain.event.StructuredFormUpdatedForDefendant;
import uk.gov.moj.cpp.material.persistence.constant.StructuredFormStatus;
import uk.gov.moj.cpp.material.persistence.entity.StructuredForm;
import uk.gov.moj.cpp.material.persistence.entity.StructuredFormChangeHistory;
import uk.gov.moj.cpp.material.persistence.repository.StructuredFormChangeHistoryRepository;
import uk.gov.moj.cpp.material.persistence.repository.StructuredFormRepository;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.slf4j.Logger;

@ServiceComponent(EVENT_LISTENER)
public class StructuredFormEventListener {

    private static final Logger LOGGER = getLogger(StructuredFormEventListener.class);
    public static final String DATA = "data";
    public static final String DEFENDANT_DATA = "defendantData";
    public static final String PROSECUTION = "prosecution";
    public static final String WITNESSES = "witnesses";
    public static final String DATA_PROSECUTION_WITNESSES_ID = "$.data.prosecution.witnesses[?(@.id == \"";

    @Inject
    JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    StructuredFormRepository structuredFormRepository;

    @Inject
    private StructuredFormChangeHistoryRepository structuredFormChangeHistoryRepository;

    @Handles("material.events.structured-form-created")
    public void structuredFormCreated(final JsonEnvelope event) {
        LOGGER.info("material.events.structured-form-created event received: {}", event);

        final StructuredFormCreated structuredFormCreated = jsonObjectConverter.convert(
                event.payloadAsJsonObject(),
                StructuredFormCreated.class);

        final StructuredFormStatus structuredFormStatus = StructuredFormStatus.valueFor(structuredFormCreated.getStatus().name());

        final StructuredForm structuredForm = new StructuredForm(structuredFormCreated.getStructuredFormId(), structuredFormCreated.getFormId(), structuredFormCreated.getStructuredFormData(), structuredFormStatus, structuredFormCreated.getLastUpdated());

        structuredFormRepository.save(structuredForm);

        if (structuredFormStatus != StructuredFormStatus.DRAFTED) {
            final StructuredFormChangeHistory structuredFormChangeHistoryEntity = getStructuredFormChangeHistoryEntity(
                    randomUUID(),
                    structuredFormCreated.getStructuredFormId(),
                    structuredFormCreated.getFormId(),
                    null,
                    objectToJsonObjectConverter.convert(structuredFormCreated.getUpdatedBy()).toString(),
                    structuredFormCreated.getStructuredFormData(),
                    StructuredFormStatus.CREATED,
                    structuredFormCreated.getLastUpdated());
            structuredFormChangeHistoryRepository.save(structuredFormChangeHistoryEntity);
        }
    }

    @Handles("material.events.structured-form-updated")
    public void structuredFormUpdated(final JsonEnvelope event) {
        LOGGER.info("material.events.structured-form-updated event received: {}", event);

        final StructuredFormUpdated structuredFormUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), StructuredFormUpdated.class);

        final StructuredForm existingStructuredForm = structuredFormRepository.findBy(structuredFormUpdated.getStructuredFormId());
        if (Objects.nonNull(existingStructuredForm)) {
            existingStructuredForm.setData(structuredFormUpdated.getStructuredFormData());
            existingStructuredForm.setLastUpdated(structuredFormUpdated.getLastUpdated());
            structuredFormRepository.save(existingStructuredForm);

            if (existingStructuredForm.getStatus() != StructuredFormStatus.DRAFTED) {
                final StructuredFormChangeHistory structuredFormChangeHistoryEntity = getStructuredFormChangeHistoryEntity(
                        randomUUID(),
                        structuredFormUpdated.getStructuredFormId(),
                        existingStructuredForm.getFormId(),
                        null,
                        objectToJsonObjectConverter.convert(structuredFormUpdated.getUpdatedBy()).toString(),
                        structuredFormUpdated.getStructuredFormData(),
                        StructuredFormStatus.UPDATED,
                        structuredFormUpdated.getLastUpdated());
                structuredFormChangeHistoryRepository.save(structuredFormChangeHistoryEntity);
            }
        } else {
            LOGGER.error("StructuredForm form to be updated with id {} is not found", structuredFormUpdated.getStructuredFormId());
        }
    }

    @SuppressWarnings({"java:S1192", "squid:S134"})
    @Handles("material.events.structured-form-updated-for-defendant")
    public void structuredFormUpdatedForDefendant(final JsonEnvelope event) {
        LOGGER.info("material.events.structured-form-updated-for-defendant event received: {}", event);

        final StructuredFormUpdatedForDefendant structuredFormUpdatedForDefendant = jsonObjectConverter.convert(event.payloadAsJsonObject(), StructuredFormUpdatedForDefendant.class);

        final StructuredForm existingStructuredFormEntity = structuredFormRepository.findBy(structuredFormUpdatedForDefendant.getStructuredFormId());
        if (Objects.nonNull(existingStructuredFormEntity)) {

            final JsonObject existingStructuredFormJson = objectToJsonObjectConverter.convert(existingStructuredFormEntity);

            final JsonObject defendantData = stringToJsonObjectConverter.convert(event.payloadAsJsonObject().getString(DEFENDANT_DATA));

            // update defendant information
            final Object updatedData = JsonPath.parse(existingStructuredFormJson.getString(DATA))
                    .set("$.data.defence.defendants[?(@.id == \"" + structuredFormUpdatedForDefendant.getDefendantId().toString() + "\")]", stringToJsonObjectConverter.convert(defendantData.getJsonObject("defendant").toString()))
                    .json();

            // store as string so we can parse with jsonpath
            String updatedFormData = objectToJsonObjectConverter.convert(updatedData).toString();

            if (defendantData.containsKey(PROSECUTION) && defendantData.getJsonObject(PROSECUTION).containsKey(WITNESSES)) {

                final List<String> witnessIdsToBeUpdated = JsonPath.parse(defendantData.getJsonObject(PROSECUTION).toString()).read("$.witnesses[*].id");

                for (final String witnessIdToBeUpdated : witnessIdsToBeUpdated) {

                    final String crossExaminationResponse = objectToJsonObjectConverter.convert(((JSONArray) JsonPath.parse(defendantData.getJsonObject(PROSECUTION).toString()).read("$.witnesses[?(@.id == \"" + witnessIdToBeUpdated + "\")].xExamination")).get(0)).toString();

                    final JSONArray existingCrossExaminationForDefendant = JsonPath.parse(updatedFormData)
                            .read(DATA_PROSECUTION_WITNESSES_ID + witnessIdToBeUpdated + "\")].xExaminations[?(@.defendantId == \"" + structuredFormUpdatedForDefendant.getDefendantId().toString() + "\")]");

                    if (!existingCrossExaminationForDefendant.isEmpty()) {

                        final Object updatedWitness = JsonPath.parse(updatedFormData)
                                .set(DATA_PROSECUTION_WITNESSES_ID + witnessIdToBeUpdated + "\")].xExaminations[?(@.defendantId == \"" + structuredFormUpdatedForDefendant.getDefendantId().toString() + "\")]", stringToJsonObjectConverter.convert(crossExaminationResponse))
                                .json();
                        updatedFormData = objectToJsonObjectConverter.convert(updatedWitness).toString();

                    } else {

                        final DocumentContext document = JsonPath.parse(updatedFormData);
                        final JsonPath pathToCrossExaminationArray = JsonPath.compile(DATA_PROSECUTION_WITNESSES_ID + witnessIdToBeUpdated + "\")].xExaminations");
                        final JsonObject xExamJson = stringToJsonObjectConverter.convert(crossExaminationResponse);

                        final HashMap<String, Object> newCrossExam = new HashMap<>();
                        newCrossExam.put("defendantId", xExamJson.getString("defendantId"));
                        newCrossExam.put("attendInPerson", xExamJson.getBoolean("attendInPerson"));
                        newCrossExam.put("whatIsDisputed", xExamJson.getString("whatIsDisputed"));

                        document.add(pathToCrossExaminationArray, newCrossExam);
                        updatedFormData = document.jsonString();
                    }
                }
            }

            existingStructuredFormEntity.setData(updatedFormData);
            existingStructuredFormEntity.setLastUpdated(structuredFormUpdatedForDefendant.getLastUpdated());
            structuredFormRepository.save(existingStructuredFormEntity);

            if (existingStructuredFormEntity.getStatus() != StructuredFormStatus.DRAFTED) {
                final StructuredFormChangeHistory structuredFormChangeHistoryEntity = getStructuredFormChangeHistoryEntity(
                        randomUUID(),
                        structuredFormUpdatedForDefendant.getStructuredFormId(),
                        existingStructuredFormEntity.getFormId(),
                        null,
                        objectToJsonObjectConverter.convert(structuredFormUpdatedForDefendant.getUpdatedBy()).toString(),
                        existingStructuredFormEntity.getData(),
                        StructuredFormStatus.UPDATED,
                        structuredFormUpdatedForDefendant.getLastUpdated());
                structuredFormChangeHistoryRepository.save(structuredFormChangeHistoryEntity);
            }

        } else {
            LOGGER.error("StructuredForm form to be updated with id {} is not found", structuredFormUpdatedForDefendant.getStructuredFormId());
        }
    }

    @Handles("material.events.structured-form-published")
    public void structuredFormPublished(final JsonEnvelope event) {
        LOGGER.info("material.events.structured-form-published event received: {}", event);

        final StructuredFormPublished structuredFormPublished = jsonObjectConverter.convert(event.payloadAsJsonObject(), StructuredFormPublished.class);

        final StructuredForm existingStructuredForm = structuredFormRepository.findBy(structuredFormPublished.getStructuredFormId());
        if (Objects.nonNull(existingStructuredForm)) {
            existingStructuredForm.setStatus(StructuredFormStatus.PUBLISHED);
            existingStructuredForm.setLastUpdated(structuredFormPublished.getLastUpdated());
            structuredFormRepository.save(existingStructuredForm);

            final StructuredFormChangeHistory structuredFormChangeHistoryEntity = getStructuredFormChangeHistoryEntity(
                    randomUUID(),
                    structuredFormPublished.getStructuredFormId(),
                    existingStructuredForm.getFormId(),
                    null,
                    objectToJsonObjectConverter.convert(structuredFormPublished.getUpdatedBy()).toString(),
                    existingStructuredForm.getData(),
                    StructuredFormStatus.PUBLISHED,
                    structuredFormPublished.getLastUpdated());
            structuredFormChangeHistoryRepository.save(structuredFormChangeHistoryEntity);
        } else {
            LOGGER.error("StructuredForm form to be published with id {} is not found", structuredFormPublished.getStructuredFormId());
        }
    }

    @Handles("material.events.structured-form-finalised")
    public void structuredFormFinalised(final JsonEnvelope event) {
        LOGGER.info("material.events.structured-form-finalised event received: {}", event);

        final StructuredFormFinalised structuredFormFinalised = jsonObjectConverter.convert(event.payloadAsJsonObject(), StructuredFormFinalised.class);

        final StructuredForm existingStructuredForm = structuredFormRepository.findBy(structuredFormFinalised.getStructuredFormId());
        if (Objects.nonNull(existingStructuredForm)) {
            existingStructuredForm.setStatus(StructuredFormStatus.FINALISED);
            existingStructuredForm.setLastUpdated(structuredFormFinalised.getLastUpdated());
            structuredFormRepository.save(existingStructuredForm);

            final StructuredFormChangeHistory structuredFormChangeHistoryEntity = getStructuredFormChangeHistoryEntity(
                    randomUUID(),
                    structuredFormFinalised.getStructuredFormId(),
                    existingStructuredForm.getFormId(),
                    structuredFormFinalised.getMaterialId(),
                    objectToJsonObjectConverter.convert(structuredFormFinalised.getUpdatedBy()).toString(),
                    existingStructuredForm.getData(),
                    StructuredFormStatus.FINALISED,
                    structuredFormFinalised.getLastUpdated());
            structuredFormChangeHistoryRepository.save(structuredFormChangeHistoryEntity);
        } else {
            LOGGER.error("StructuredForm form to be finalised with id {} is not found", structuredFormFinalised.getStructuredFormId());
        }
    }

    @SuppressWarnings("java:S107")
    private StructuredFormChangeHistory getStructuredFormChangeHistoryEntity(final UUID id, final UUID structuredFormId, final UUID formId, final UUID materialId, final String updatedBy, final String data, final StructuredFormStatus status, final ZonedDateTime lastUpdated) {
        return StructuredFormChangeHistory.builder()
                .withId(id)
                .withStructuredFormId(structuredFormId)
                .withFormId(formId)
                .withMaterialId(materialId)
                .withDate(lastUpdated)
                .withUpdatedBy(updatedBy)
                .withData(data)
                .withStatus(status)
                .build();
    }
}
