package uk.gov.moj.cpp.material.query.view;

import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.material.query.view.StructuredFormQueryView.ZONE_DATETIME_FORMATTER;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.persistence.constant.StructuredFormStatus;
import uk.gov.moj.cpp.material.persistence.entity.StructuredFormChangeHistory;
import uk.gov.moj.cpp.material.persistence.repository.StructuredFormChangeHistoryRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StructuredFormChangeHistoryQueryViewTest {

    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String ID = "id";
    public static final String NAME = "name";
    @InjectMocks
    private StructuredFormChangeHistoryQueryView structuredFormChangeHistoryQueryView;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private StructuredFormChangeHistoryRepository structuredFormChangeHistoryRepository;

    private ZonedDateTime updateDate1 = now();
    private ZonedDateTime updateDate2 = now().minusDays(1);
    private ZonedDateTime updateDate3 = now().minusDays(2);

    private String updatedBy1 = randomUUID().toString();
    private String updatedByObject1 = "{\"id\":" + "\"" + updatedBy1 + "\"" + ",\"firstName\":\"Bill\",\"lastName\":\"Turner\"}";
    private String updatedBy2 = randomUUID().toString();
    private String updatedByObject2 = "{\"id\":" + "\"" + updatedBy2 + "\"" + ",\"firstName\":\"Elizabeth\",\"lastName\":\"Turner\"}";
    private String updatedBy3 = randomUUID().toString();
    private String updatedByObject3 = "{\"id\":" + "\"" + updatedBy3 + "\"" + ",\"firstName\":\"Jack\",\"lastName\":\"Sparrow\"}";

    private UUID id1 = randomUUID();
    private UUID id2 = randomUUID();
    private UUID id3 = randomUUID();

    private UUID materialId = randomUUID();
    private UUID structuredFormId = randomUUID();
    private UUID formId = randomUUID();

    private String data1 = "{\"firstName\":\"Bill\",\"lastName\":\"Turner\"}";
    private String data2 = "{\"firstName\":\"Elizabeth\",\"lastName\":\"Turner\"}";
    private String data3 = "{\"firstName\":\"Jack\",\"lastName\":\"Sparrow\"}";

    @BeforeEach
    public void setUp() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.structuredFormChangeHistoryQueryView, "stringToJsonObjectConverter", new StringToJsonObjectConverter());
    }

    @Test
    public void shouldReturnStructuredFormChangeHistoryWithUpdatedByCpUser() {
        when(structuredFormChangeHistoryRepository.findByStructuredFormId(structuredFormId)).thenReturn(asList(
                createStructuredFormChangeHistoryEntity(id1, null, updateDate1, updatedByObject1, data1),
                createStructuredFormChangeHistoryEntity(id2, null, updateDate2, updatedByObject2, data2),
                createStructuredFormChangeHistoryEntity(id3, materialId, updateDate3, updatedByObject3, data3)
        ));


        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadataWithDefaults().withName("material.query.structured-form-change-history"), createObjectBuilder().add("structuredFormId", structuredFormId.toString()).build());
        final JsonEnvelope result = structuredFormChangeHistoryQueryView.getStructuredFormChangeHistory(envelope);

        final JsonArray structuredFormChangeHistoryArray = result.payloadAsJsonObject().getJsonArray("structuredFormChangeHistory");
        assertThat(structuredFormChangeHistoryArray.size(), is(3));

        final JsonObject structuredFormChangeHistory1 = structuredFormChangeHistoryArray.getJsonObject(0);
        assertThat(structuredFormChangeHistory1.getString(ID), is(id1.toString()));
        assertThat(structuredFormChangeHistory1.getString("structuredFormId"), is(structuredFormId.toString()));
        assertThat(structuredFormChangeHistory1.getString("formId"), is(formId.toString()));
        assertThat(structuredFormChangeHistory1.containsKey("materialId"), is(false));
        assertThat(structuredFormChangeHistory1.getString("date"), is(updateDate1.format(ZONE_DATETIME_FORMATTER)));
        assertThat(structuredFormChangeHistory1.getJsonObject("updatedBy").getString(ID), is(updatedBy1));
        assertThat(structuredFormChangeHistory1.getJsonObject("updatedBy").getString(FIRST_NAME), is("Bill"));
        assertThat(structuredFormChangeHistory1.getJsonObject("updatedBy").getString(LAST_NAME), is("Turner"));
        assertThat(structuredFormChangeHistory1.getString("data"), is(data1));
        assertThat(structuredFormChangeHistory1.getString("status"), is(StructuredFormStatus.CREATED.name()));


        final JsonObject structuredFormChangeHistory2 = structuredFormChangeHistoryArray.getJsonObject(1);
        assertThat(structuredFormChangeHistory2.getString(ID), is(id2.toString()));
        assertThat(structuredFormChangeHistory2.getString("structuredFormId"), is(structuredFormId.toString()));
        assertThat(structuredFormChangeHistory2.getString("formId"), is(formId.toString()));
        assertThat(structuredFormChangeHistory2.containsKey("materialId"), is(false));
        assertThat(structuredFormChangeHistory2.getString("date"), is(updateDate2.format(ZONE_DATETIME_FORMATTER)));
        assertThat(structuredFormChangeHistory2.getJsonObject("updatedBy").getString(ID), is(updatedBy2));
        assertThat(structuredFormChangeHistory2.getJsonObject("updatedBy").getString(FIRST_NAME), is("Elizabeth"));
        assertThat(structuredFormChangeHistory2.getJsonObject("updatedBy").getString(LAST_NAME), is("Turner"));
        assertThat(structuredFormChangeHistory2.getString("data"), is(data2));
        assertThat(structuredFormChangeHistory2.getString("status"), is(StructuredFormStatus.CREATED.name()));

        final JsonObject structuredFormChangeHistory3 = structuredFormChangeHistoryArray.getJsonObject(2);
        assertThat(structuredFormChangeHistory3.getString(ID), is(id3.toString()));
        assertThat(structuredFormChangeHistory3.getString("structuredFormId"), is(structuredFormId.toString()));
        assertThat(structuredFormChangeHistory3.getString("formId"), is(formId.toString()));
        assertThat(structuredFormChangeHistory3.getString("materialId"), is(materialId.toString()));
        assertThat(structuredFormChangeHistory3.getString("date"), is(updateDate3.format(ZONE_DATETIME_FORMATTER)));
        assertThat(structuredFormChangeHistory3.getJsonObject("updatedBy").getString(ID), is(updatedBy3));
        assertThat(structuredFormChangeHistory3.getJsonObject("updatedBy").getString(FIRST_NAME), is("Jack"));
        assertThat(structuredFormChangeHistory3.getJsonObject("updatedBy").getString(LAST_NAME), is("Sparrow"));
        assertThat(structuredFormChangeHistory3.getString("data"), is(data3));
        assertThat(structuredFormChangeHistory3.getString("status"), is(StructuredFormStatus.CREATED.name()));
    }

    @Test
    public void shouldReturnStructuredFormChangeHistoryWhenUpdatedByCpsUser() {
        updatedByObject1 = "{\"name\":\"Mr Bill Turner\"}";
        updatedByObject2 = "{\"name\":\"Mrs Elizabeth Turner\"}";
        updatedByObject3 = "{\"name\":\"Mr Jack Sparrow\"}";


        when(structuredFormChangeHistoryRepository.findByStructuredFormId(structuredFormId)).thenReturn(asList(
                createStructuredFormChangeHistoryEntity(id1, null, updateDate1, updatedByObject1, data1),
                createStructuredFormChangeHistoryEntity(id2, null, updateDate2, updatedByObject2, data2),
                createStructuredFormChangeHistoryEntity(id3, materialId, updateDate3, updatedByObject3, data3)
        ));


        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadataWithDefaults().withName("material.query.structured-form-change-history"), createObjectBuilder().add("structuredFormId", structuredFormId.toString()).build());
        final JsonEnvelope result = structuredFormChangeHistoryQueryView.getStructuredFormChangeHistory(envelope);

        final JsonArray structuredFormChangeHistoryArray = result.payloadAsJsonObject().getJsonArray("structuredFormChangeHistory");
        assertThat(structuredFormChangeHistoryArray.size(), is(3));

        final JsonObject structuredFormChangeHistory1 = structuredFormChangeHistoryArray.getJsonObject(0);
        assertThat(structuredFormChangeHistory1.getString(ID), is(id1.toString()));
        assertThat(structuredFormChangeHistory1.getString("structuredFormId"), is(structuredFormId.toString()));
        assertThat(structuredFormChangeHistory1.getString("formId"), is(formId.toString()));
        assertThat(structuredFormChangeHistory1.containsKey("materialId"), is(false));
        assertThat(structuredFormChangeHistory1.getString("date"), is(updateDate1.format(ZONE_DATETIME_FORMATTER)));
        assertThat(structuredFormChangeHistory1.getJsonObject("updatedBy").containsKey(ID), is(false));
        assertThat(structuredFormChangeHistory1.getJsonObject("updatedBy").getString(NAME), is("Mr Bill Turner"));
        assertThat(structuredFormChangeHistory1.getString("data"), is(data1));
        assertThat(structuredFormChangeHistory1.getString("status"), is(StructuredFormStatus.CREATED.name()));


        final JsonObject structuredFormChangeHistory2 = structuredFormChangeHistoryArray.getJsonObject(1);
        assertThat(structuredFormChangeHistory2.getString(ID), is(id2.toString()));
        assertThat(structuredFormChangeHistory2.getString("structuredFormId"), is(structuredFormId.toString()));
        assertThat(structuredFormChangeHistory2.getString("formId"), is(formId.toString()));
        assertThat(structuredFormChangeHistory1.containsKey("materialId"), is(false));
        assertThat(structuredFormChangeHistory2.getString("date"), is(updateDate2.format(ZONE_DATETIME_FORMATTER)));
        assertThat(structuredFormChangeHistory2.getJsonObject("updatedBy").containsKey(ID), is(false));
        assertThat(structuredFormChangeHistory2.getJsonObject("updatedBy").getString(NAME), is("Mrs Elizabeth Turner"));
        assertThat(structuredFormChangeHistory2.getString("data"), is(data2));
        assertThat(structuredFormChangeHistory2.getString("status"), is(StructuredFormStatus.CREATED.name()));

        final JsonObject structuredFormChangeHistory3 = structuredFormChangeHistoryArray.getJsonObject(2);
        assertThat(structuredFormChangeHistory3.getString(ID), is(id3.toString()));
        assertThat(structuredFormChangeHistory3.getString("structuredFormId"), is(structuredFormId.toString()));
        assertThat(structuredFormChangeHistory3.getString("formId"), is(formId.toString()));
        assertThat(structuredFormChangeHistory3.getString("materialId"), is(materialId.toString()));
        assertThat(structuredFormChangeHistory3.getString("date"), is(updateDate3.format(ZONE_DATETIME_FORMATTER)));
        assertThat(structuredFormChangeHistory3.getJsonObject("updatedBy").containsKey(ID), is(false));
        assertThat(structuredFormChangeHistory3.getJsonObject("updatedBy").getString(NAME), is("Mr Jack Sparrow"));
        assertThat(structuredFormChangeHistory3.getString("data"), is(data3));
        assertThat(structuredFormChangeHistory3.getString("status"), is(StructuredFormStatus.CREATED.name()));
    }

    @Test
    public void shouldReturnStructuredFormChangeHistoryWithAtLeastOneTypeOfUser() {

        updatedByObject1 = "{}";
        updatedByObject2 = "{}";
        updatedByObject3 = "{}";

        final List<StructuredFormChangeHistory> historyList = asList(
                createStructuredFormChangeHistoryEntity(id1, null, updateDate1, updatedByObject1, data1),
                createStructuredFormChangeHistoryEntity(id2, null, updateDate2, updatedByObject2, data2),
                createStructuredFormChangeHistoryEntity(id3, materialId, updateDate3, updatedByObject3, data3)
        );

        when(structuredFormChangeHistoryRepository.findByStructuredFormId(structuredFormId)).thenReturn(historyList);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadataWithDefaults().withName("material.query.structured-form-change-history"), createObjectBuilder().add("structuredFormId", structuredFormId.toString()).build());
        assertThrows(NullPointerException.class, () -> structuredFormChangeHistoryQueryView.getStructuredFormChangeHistory(envelope));
    }


    @Test
    public void shouldReturnStructuredFormChangeHistoryWhenCpNamesNotPresent() {

        updatedBy1 = randomUUID().toString();
        updatedByObject1 = "{\"id\":" + "\"" + updatedBy1 + "\"" + "}";
        updatedBy2 = randomUUID().toString();
        updatedByObject2 = "{\"id\":" + "\"" + updatedBy2 + "\"" + "}";
        updatedBy3 = randomUUID().toString();
        updatedByObject3 = "{\"id\":" + "\"" + updatedBy3 + "\"" + "}";

        when(structuredFormChangeHistoryRepository.findByStructuredFormId(structuredFormId)).thenReturn(asList(
                createStructuredFormChangeHistoryEntity(id1, null, updateDate1, updatedByObject1, data1),
                createStructuredFormChangeHistoryEntity(id2, null, updateDate2, updatedByObject2, data2),
                createStructuredFormChangeHistoryEntity(id3, materialId, updateDate3, updatedByObject3, data3)
        ));


        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadataWithDefaults().withName("material.query.structured-form-change-history"), createObjectBuilder().add("structuredFormId", structuredFormId.toString()).build());

        assertThrows(NullPointerException.class , () ->  structuredFormChangeHistoryQueryView.getStructuredFormChangeHistory(envelope));
    }

    private StructuredFormChangeHistory createStructuredFormChangeHistoryEntity(final UUID id, final UUID materialId, final ZonedDateTime date, final String updatedBy, final String data) {
        return new StructuredFormChangeHistory(id, structuredFormId, formId, materialId, date, updatedBy, data, StructuredFormStatus.CREATED);
    }
}
