package uk.gov.moj.material.it.test;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.material.it.helper.StructuredFormTestHelper.FINALISED;
import static uk.gov.moj.material.it.helper.StructuredFormTestHelper.USER_ID;
import static uk.gov.moj.material.it.helper.TestHelper.postMessageToTopicAndVerify;
import static uk.gov.moj.material.it.util.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.material.it.helper.StructuredFormTestHelper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StructuredFormIT extends BaseIT {

    private static StructuredFormTestHelper testHelper;
    final private StringToJsonObjectConverter jsonObjectConverter = new StringToJsonObjectConverter();
    private static JmsMessageConsumerClient consumer;

    @BeforeEach
    public void setUp() {
        testHelper = new StructuredFormTestHelper();
        testHelper.setup();
    }

    @BeforeAll
    public static void setUpAll() {
        testHelper = new StructuredFormTestHelper();
        testHelper.setup();

        consumer = newPublicJmsMessageConsumerClientProvider()
                .withEventNames("public.material.structured-form-operation-successful")
                .getMessageConsumerClient();
    }

    @Test
    public void shouldCreateAndUpdateStructuredForm() {
        final UUID caseId = randomUUID();
        final UUID structuredFormId = randomUUID();
        final UUID formId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID materialId = randomUUID();
        int expectedHistoryItemCount = 0;
        final String CPS_ACTOR_NAME_FOR_FINALISING_PET_FORM = "Cps User";

        // create the pet form
        expectedHistoryItemCount++;

        final String progressionPetFormCreatedPayload = getPayload("stub-data/received-public-events/public.progression.pet-form-created.json")
                .replaceAll("CASE_ID", caseId.toString())
                .replaceAll("PET_ID", structuredFormId.toString())
                .replaceAll("FORM_ID", formId.toString())
                .replaceAll("DEFENDANT_ID", defendantId.toString())
                .replaceAll("USER_ID", USER_ID);
        postMessageToTopicAndVerify(progressionPetFormCreatedPayload, "material.events.structured-form-created", "public.progression.pet-form-created", true);

        // verify pet form created
        testHelper.queryAndVerifyStructuredFormDetail(structuredFormId, formId, 2);

        final String progressionPetFormUpdatedPayload = getPayload("stub-data/received-public-events/public.progression.pet-form-updated.json")
                .replaceAll("CASE_ID", caseId.toString())
                .replaceAll("PET_ID", structuredFormId.toString())
                .replaceAll("FORM_ID", formId.toString())
                .replaceAll("DEFENDANT_ID", defendantId.toString())
                .replaceAll("USER_ID", USER_ID);

        // update the pet form
        postMessageToTopicAndVerify(progressionPetFormUpdatedPayload, "material.events.structured-form-updated", "public.progression.pet-form-updated", true);

        // verify pet form updated
        expectedHistoryItemCount++;
        testHelper.queryAndVerifyStructuredFormDetailAfterUpdate(structuredFormId, formId, 2);

        // update the defendant info in pet form
        final String progressionPetFormUpdatedForDefendantPayload = getPayload("stub-data/received-public-events/public.progression.pet-form-defendant-updated.json")
                .replaceAll("CASE_ID", caseId.toString())
                .replaceAll("PET_ID", structuredFormId.toString())
                .replaceAll("DEFENDANT_ID", defendantId.toString())
                .replaceAll("USER_ID", USER_ID);

        expectedHistoryItemCount++;
        postMessageToTopicAndVerify(progressionPetFormUpdatedForDefendantPayload, "material.events.structured-form-updated-for-defendant", "public.progression.pet-form-defendant-updated", true);

        // verify the changes to the defendant
        testHelper.queryAndVerifyStructuredFormDetailAfterDefendantUpdate(structuredFormId, formId);

        //receive finalise pet form public event
        expectedHistoryItemCount++;
        final String suspect = getPayload("stub-data/received-public-events/public.progression.pet-form-finalised.json")
                .replace("PET_ID", structuredFormId.toString())
                .replace("CASE_ID", randomUUID().toString())
                .replace("MATERIAL_ID", materialId.toString())
                .replace("ACTOR_NAME", CPS_ACTOR_NAME_FOR_FINALISING_PET_FORM);

        postMessageToTopicAndVerify(suspect, "material.events.structured-form-finalised", "public.progression.pet-form-finalised", true);

        //retrieve history
        final JsonObject historyPayload = testHelper.queryStructuredFormHistory(structuredFormId);
        final JsonArray historyArray = historyPayload.getJsonArray("structuredFormChangeHistory");
        assertThat(historyArray.size(), is(expectedHistoryItemCount));
        final Optional<JsonObject> finalisedHistoryItem = historyArray.stream()
                .map(jsonValue -> (JsonObject) jsonValue)
                .filter(o -> o.getString("status").equals("FINALISED"))
                .findFirst();
        assertThat(finalisedHistoryItem.isPresent(), is(true));
        assertThat(finalisedHistoryItem.get().getString("materialId"), is(materialId.toString()));
        final JsonObject updatedBy = finalisedHistoryItem.get().getJsonObject("updatedBy");
        assertThat(updatedBy.getString("name"), is(CPS_ACTOR_NAME_FOR_FINALISING_PET_FORM));

        // get the defendant user
        testHelper.queryAndVerifyStructuredFormDefendantUser(structuredFormId, defendantId);
    }

    @Test
    public void should_Create_Update_Finalise_StructuredFormForBCM() {
        final UUID caseId = randomUUID();
        final UUID structuredFormId = randomUUID();
        final UUID formId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID materialId1 = randomUUID();
        final UUID materialId2 = randomUUID();
        final UUID materialId3 = randomUUID();
        final List<String> expectedMaterialList = asList(materialId1.toString(), materialId2.toString(), materialId3.toString());

        int expectedHistoryItemCount = 0;

        final String progressionFormCreatedPayload = getPayload("stub-data/received-public-events/public.progression.form-created.json")
                .replaceAll("<CASE_ID>", caseId.toString())
                .replaceAll("<COURT_FORM_ID>", structuredFormId.toString())
                .replaceAll("<FORM_ID>", formId.toString())
                .replaceAll("<DEFENDANT_ID>", defendantId.toString())
                .replaceAll("<USER_ID>", USER_ID);
        postMessageToTopicAndVerify(progressionFormCreatedPayload, "material.events.structured-form-created", "public.progression.form-created", true);

        // verify form created
        expectedHistoryItemCount++;
        testHelper.queryAndVerifyStructuredFormDetail(structuredFormId, formId, 2);

        verifyConsumerMessage(consumer);

        // update the form
        final String progressionFormUpdatedPayload = getPayload("stub-data/received-public-events/public.progression.form-updated.json")
                .replaceAll("<CASE_ID>", caseId.toString())
                .replaceAll("<COURT_FORM_ID>", structuredFormId.toString())
                .replaceAll("<USER_ID>", USER_ID);

        postMessageToTopicAndVerify(progressionFormUpdatedPayload, "material.events.structured-form-updated", "public.progression.form-updated", true);

        // verify form updated
        expectedHistoryItemCount++;
        testHelper.queryAndVerifyStructuredFormDetailAfterFormUpdate(structuredFormId, formId);

        final String suspect = getPayload("stub-data/received-public-events/public.progression.form-finalised.json")
                .replace("COURT_FORM_ID", structuredFormId.toString())
                .replace("CASE_ID", caseId.toString())
                .replace("MATERIAL_ID1", materialId1.toString())
                .replace("MATERIAL_ID2", materialId2.toString())
                .replace("MATERIAL_ID3", materialId3.toString())
                .replace("USER_ID", USER_ID);

        postMessageToTopicAndVerify(suspect, "material.events.structured-form-finalised", "public.progression.form-finalised", true);

        // verify form finalised
        expectedHistoryItemCount++;
        testHelper.queryAndVerifyStructuredFormDetailAfterFormFinalised(structuredFormId, formId);

        //retrieve history
        final JsonObject historyPayload = testHelper.queryStructuredFormHistory(structuredFormId);
        final JsonArray historyArray = historyPayload.getJsonArray("structuredFormChangeHistory");
        assertThat(historyArray.size(), is(expectedHistoryItemCount + 2));

        final List<JsonObject> status = historyArray.stream()
                .map(jsonValue -> (JsonObject) jsonValue)
                .filter(o -> o.getString("status").equals(FINALISED))
                .toList();

        final List<String> actualMaterialIds = status.stream().map(e -> e.getString("materialId")).collect(toList());

        assertThat(actualMaterialIds, hasSize(expectedMaterialList.size()));
        assertThat(actualMaterialIds, containsInAnyOrder(expectedMaterialList.toArray()));
    }

    @Test
    public void should_Create_Update_Finalise_StructuredFormForBCM_FromCPS() {
        final UUID caseId = randomUUID();
        final UUID structuredFormId = randomUUID();
        final UUID formId = randomUUID();
        final UUID defendantId = randomUUID();
        final String CPS_ACTOR_NAME_FOR_CREATE_FORM = "Cps formCreator";


        final String progressionFormCreatedPayload = getPayload("stub-data/received-public-events/public.progression.cps-form-created.json")
                .replaceAll("<CASE_ID>", caseId.toString())
                .replaceAll("<COURT_FORM_ID>", structuredFormId.toString())
                .replaceAll("<FORM_ID>", formId.toString())
                .replaceAll("<DEFENDANT_ID>", defendantId.toString());
        postMessageToTopicAndVerify(progressionFormCreatedPayload, "material.events.structured-form-created", "public.progression.form-created", true);

        // verify form created
        testHelper.queryAndVerifyStructuredFormDetail(structuredFormId, formId, 2);
        verifyConsumerMessage(consumer);

        final JsonObject historyPayload = testHelper.queryStructuredFormHistory(structuredFormId);
        final JsonArray historyArray = historyPayload.getJsonArray("structuredFormChangeHistory");
        final Optional<JsonObject> createdHistoryItem = historyArray.stream()
                .map(jsonValue -> (JsonObject) jsonValue)
                .filter(o -> o.getString("status").equals("CREATED"))
                .findFirst();
        assertThat(createdHistoryItem.isPresent(), is(true));
        final JsonObject updatedBy = createdHistoryItem.get().getJsonObject("updatedBy");
        assertThat(updatedBy.getString("name"), is(CPS_ACTOR_NAME_FOR_CREATE_FORM));

    }


    private void verifyConsumerMessage(final JmsMessageConsumerClient consumer) {
        final JsonObject convert = jsonObjectConverter.convert(consumer.retrieveMessage().get());
        assertThat(convert, is(notNullValue()));
        assertThat(convert.getString("command"), is("structured-form-created"));
    }

}
