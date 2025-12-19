package uk.gov.moj.material.it.test;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.within;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.material.it.dataaccess.MaterialDataAccessor;
import uk.gov.moj.material.it.dataaccess.MaterialReference;
import uk.gov.moj.material.it.helper.MaterialRestClient;
import uk.gov.moj.material.it.util.WiremockAccessControlEndpointStubber;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class AddMaterialReferenceIT extends BaseIT {

    private final UUID userId = randomUUID();

    private final MaterialRestClient materialRestClient = new MaterialRestClient();
    private static final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private final WiremockAccessControlEndpointStubber wiremockAccessControlEndpointStubber = new WiremockAccessControlEndpointStubber();

    private final MaterialDataAccessor materialDataAccessor = new MaterialDataAccessor();
    private final Poller poller = new Poller();
    final String addMaterialCommandName = "material.add-material";


    @BeforeAll
    public static void cleanDatabase() {
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "material");
    }

    @BeforeEach
    public void stubAccessControlWithWiremock() {
        wiremockAccessControlEndpointStubber.stubStructureAsCPSProsecutedCase();
        wiremockAccessControlEndpointStubber.stubUsersAndGroupsUserAsSystemUser(userId.toString());
        wiremockAccessControlEndpointStubber.setupLoggedInUsersPermissionQueryStub(userId.toString());
    }

    @Test
    public void shouldAcceptAndProcessTheAddMaterialReferenceCommand() {

        final UUID materialId = randomUUID();
        final UUID fileReference = randomUUID();

        final JsonObject payload = createAddMaterialPayload(materialId, fileReference);

        materialRestClient.postCommand(userId, addMaterialCommandName, payload);

        final Optional<MaterialReference> materialReference = poller.pollUntilFound(() -> materialDataAccessor.get(materialId));

        assertMaterialAdded(materialId, fileReference, materialReference);
    }

    @Test
    public void shouldAcceptAndProcessTheAddMaterialReferenceCommandWithGroupsNotPermission() {
        wiremockAccessControlEndpointStubber.setupLoggedInUsersPermissionQueryStubV1(userId.toString());

        final UUID materialId = randomUUID();
        final UUID fileReference = randomUUID();

        final JsonObject payload = createAddMaterialPayload(materialId, fileReference);

        materialRestClient.postCommand(userId, addMaterialCommandName, payload);

        final Optional<MaterialReference> materialReference = poller.pollUntilFound(() -> materialDataAccessor.get(materialId));

        assertMaterialAdded(materialId, fileReference, materialReference);
    }

    private void assertMaterialAdded(final UUID materialId, final UUID fileReference,
                                     final Optional<MaterialReference> materialReference) {
        assertThat(materialReference.isPresent(), is(true));

        assertThat(materialReference.get().getMaterialId(), is(materialId));
        assertThat(materialReference.get().getFileReference(), is(fileReference.toString()));
        assertThat(materialReference.get().getFileName(), is("Boys' Book of Fun.pdf"));
        assertThat(materialReference.get().getMimeType(), is("application/pdf"));
        assertThat(materialReference.get().getExternalLink(), is(nullValue()));

        assertThat(materialReference.get().getDateAdded(), within(2, MINUTES, new UtcClock().now()));
    }

    @Test
    public void shouldRaiseDuplicateMaterialEvent() {

        final JmsMessageConsumerClient publicMessageConsumer = newPublicJmsMessageConsumerClientProvider()
                .withEventNames("material.duplicate-material-not-created")
                .getMessageConsumerClient();

        final UUID materialId = randomUUID();
        final UUID fileReference = randomUUID();

        final JsonObject payload = createAddMaterialPayload(materialId, fileReference);


        materialRestClient.postCommand(userId, addMaterialCommandName, payload);

        final Optional<MaterialReference> materialReference = poller.pollUntilFound(() -> materialDataAccessor.get(materialId));

        assertMaterialAdded(materialId, fileReference, materialReference);

        materialRestClient.postCommand(userId, addMaterialCommandName, payload);
        assertThat(publicMessageConsumer.retrieveMessage().get(), is(notNullValue()));
    }

    private JsonObject createAddMaterialPayload(final UUID materialId, final UUID fileReference) {
        return createObjectBuilder()
                .add("materialId", materialId.toString())
                .add("fileName", "Boys' Book of Fun.pdf")
                .add("document", createObjectBuilder()
                        .add("fileReference", fileReference.toString())
                        .add("mimeType", "application/pdf"))
                .build();
    }
}
