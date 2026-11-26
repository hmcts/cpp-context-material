package uk.gov.moj.cpp.material.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.json.schemas.material.UpdatedBy;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.material.domain.StructuredFormStatus;
import uk.gov.moj.cpp.material.domain.aggregate.StructuredFormAggregate;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class StructuredFormCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredFormCommandHandler.class);

    @Inject
    EventSource eventSource;

    @Inject
    Enveloper enveloper;

    @Inject
    AggregateService aggregateService;

    @Handles("material.command.handler.create-structured-form")
    public void handleCreateStructuredForm(final Envelope<CreateStructuredForm> envelope) throws EventStreamException {
        LOGGER.debug("material.command.handler.create-structured-form {}", envelope);

        final CreateStructuredForm createStructuredForm = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(createStructuredForm.getStructuredFormId());

        final UpdatedBy updatedByFromEvent = createStructuredForm.getUpdatedBy();
        final uk.gov.moj.cpp.material.domain.UpdatedBy updatedBy = new uk.gov.moj.cpp.material.domain.UpdatedBy(updatedByFromEvent.getId(), updatedByFromEvent.getFirstName(), updatedByFromEvent.getLastName(), updatedByFromEvent.getName());

        final StructuredFormAggregate structuredFormAggregate = aggregateService.get(eventStream, StructuredFormAggregate.class);
        final Stream<Object> events = structuredFormAggregate.createStructuredForm(createStructuredForm.getStructuredFormId(),
                createStructuredForm.getFormId(),
                createStructuredForm.getStructuredFormData(),
                StructuredFormStatus.valueFor(createStructuredForm.getStatus()),
                updatedBy);

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles("material.command.handler.update-structured-form")
    public void handleUpdateStructuredForm(final Envelope<UpdateStructuredForm> envelope) throws EventStreamException {
        LOGGER.debug("material.command.handler.update-structured-form {}", envelope);

        final UpdateStructuredForm updateStructuredForm = envelope.payload();

        final UpdatedBy updatedByFromEvent = updateStructuredForm.getUpdatedBy();
        final uk.gov.moj.cpp.material.domain.UpdatedBy updatedBy = new uk.gov.moj.cpp.material.domain.UpdatedBy(updatedByFromEvent.getId(), updatedByFromEvent.getFirstName(), updatedByFromEvent.getLastName(), updatedByFromEvent.getName());

        final EventStream eventStream = eventSource.getStreamById(updateStructuredForm.getStructuredFormId());
        final StructuredFormAggregate structuredFormAggregate = aggregateService.get(eventStream, StructuredFormAggregate.class);
        final Stream<Object> events = structuredFormAggregate.updateStructuredForm(updateStructuredForm.getStructuredFormId(),
                updateStructuredForm.getStructuredFormData(),
                updatedBy);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles("material.command.handler.update-structured-form-for-defendant")
    public void handleUpdateStructuredFormForDefendant(final Envelope<UpdateStructuredFormForDefendant> envelope) throws EventStreamException {
        LOGGER.debug("material.command.handler.update-structured-form-for-defendant {}", envelope);

        final UpdateStructuredFormForDefendant updateStructuredFormForDefendant = envelope.payload();

        final UpdatedBy updatedByFromEvent = updateStructuredFormForDefendant.getUpdatedBy();
        final uk.gov.moj.cpp.material.domain.UpdatedBy updatedBy = new uk.gov.moj.cpp.material.domain.UpdatedBy(updatedByFromEvent.getId(), updatedByFromEvent.getFirstName(), updatedByFromEvent.getLastName(), updatedByFromEvent.getName());

        final EventStream eventStream = eventSource.getStreamById(updateStructuredFormForDefendant.getStructuredFormId());
        final StructuredFormAggregate structuredFormAggregate = aggregateService.get(eventStream, StructuredFormAggregate.class);
        final Stream<Object> events = structuredFormAggregate.updateStructuredFormForDefendant(updateStructuredFormForDefendant.getStructuredFormId(),
                updateStructuredFormForDefendant.getDefendantId(),
                updateStructuredFormForDefendant.getDefendantData(),
                updatedBy);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles("material.command.handler.publish-structured-form")
    public void handlePublishStructuredForm(final Envelope<PublishStructuredForm> envelope) throws EventStreamException {
        LOGGER.debug("material.command.handler.publish-structured-form {}", envelope);

        final PublishStructuredForm publishStructuredForm = envelope.payload();

        final UpdatedBy updatedByFromEvent = publishStructuredForm.getUpdatedBy();
        final uk.gov.moj.cpp.material.domain.UpdatedBy updatedBy = new uk.gov.moj.cpp.material.domain.UpdatedBy(updatedByFromEvent.getId(), updatedByFromEvent.getFirstName(), updatedByFromEvent.getLastName(), updatedByFromEvent.getName());

        final EventStream eventStream = eventSource.getStreamById(publishStructuredForm.getStructuredFormId());
        final StructuredFormAggregate structuredFormAggregate = aggregateService.get(eventStream, StructuredFormAggregate.class);
        final Stream<Object> events = structuredFormAggregate.publishStructuredForm(publishStructuredForm.getStructuredFormId(),
                updatedBy);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles("material.command.handler.finalise-structured-form")
    public void handleFinaliseStructuredForm(final Envelope<FinaliseStructuredForm> envelope) throws EventStreamException {
        LOGGER.debug("material.command.handler.finalise-structured-form {}", envelope);

        final FinaliseStructuredForm finaliseStructuredForm = envelope.payload();

        final UpdatedBy updatedByFromEvent = finaliseStructuredForm.getUpdatedBy();
        final uk.gov.moj.cpp.material.domain.UpdatedBy updatedBy = new uk.gov.moj.cpp.material.domain.UpdatedBy(updatedByFromEvent.getId(), updatedByFromEvent.getFirstName(), updatedByFromEvent.getLastName(), updatedByFromEvent.getName());

        final EventStream eventStream = eventSource.getStreamById(finaliseStructuredForm.getStructuredFormId());
        final StructuredFormAggregate structuredFormAggregate = aggregateService.get(eventStream, StructuredFormAggregate.class);
        final Stream<Object> events = structuredFormAggregate.finaliseStructuredForm(finaliseStructuredForm.getStructuredFormId(),
                finaliseStructuredForm.getMaterialId(),
                updatedBy);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }
}
