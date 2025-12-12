package uk.gov.moj.cpp.material.command.handler;

import static org.mockito.Mockito.mock;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.Tolerance;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.domain.aggregate.StructuredFormAggregate;

import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StructuredFormCommandHandlerTest {

    public static final String MATERIAL_COMMAND_HANDLER_CREATE_STRUCTURED_FORM = "material.command.handler.create-structured-form";
    public static final String MATERIAL_COMMAND_HANDLER_UPDATE_STRUCTURED_FORM = "material.command.handler.update-structured-form";
    public static final String MATERIAL_COMMAND_HANDLER_UPDATE_STRUCTURED_FORM_FOR_DEFENDANT = "material.command.handler.update-structured-form-for-defendant";
    public static final String MATERIAL_COMMAND_PUBLISH_UPDATE_STRUCTURED_FORM = "material.command.handler.publish-structured-form";
    public static final String MATERIAL_COMMAND_FINALISE_STRUCTURED_FORM = "material.command.handler.finalise-structured-form";

    @Mock
    private Enveloper enveloper;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private StructuredFormAggregate structuredFormAggregate;

    @InjectMocks
    private StructuredFormCommandHandler structuredFormCommandHandler;

    private StubEventStream stubEventStream;

    private final static JsonEnvelope fakeEnvelope = mock(JsonEnvelope.class);
    private final static Function<Object, JsonEnvelope> enveloperFunction = obj -> fakeEnvelope;

    @BeforeEach
    public void setUp() {
        stubEventStream = new StubEventStream();
    }

    @Test
    public void shouldHandleCreateStructuredFormCommand() {
        MatcherAssert.assertThat(structuredFormCommandHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleCreateStructuredForm")
                        .thatHandles(MATERIAL_COMMAND_HANDLER_CREATE_STRUCTURED_FORM)));
    }

    @Test
    public void shouldHandleUpdateStructuredFormCommand() {
        MatcherAssert.assertThat(structuredFormCommandHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleUpdateStructuredForm")
                        .thatHandles(MATERIAL_COMMAND_HANDLER_UPDATE_STRUCTURED_FORM)));
    }

    @Test
    public void shouldHandleUpdateStructuredFormForDefendantCommand() {
        MatcherAssert.assertThat(structuredFormCommandHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleUpdateStructuredFormForDefendant")
                        .thatHandles(MATERIAL_COMMAND_HANDLER_UPDATE_STRUCTURED_FORM_FOR_DEFENDANT)));
    }

    @Test
    public void shouldHandlePublishStructuredFormCommand() {
        MatcherAssert.assertThat(structuredFormCommandHandler, isHandler(COMMAND_HANDLER)
                .with(method("handlePublishStructuredForm")
                        .thatHandles(MATERIAL_COMMAND_PUBLISH_UPDATE_STRUCTURED_FORM)));
    }

    @Test
    public void shouldHandleFinaliseStructuredFormCommand() {
        MatcherAssert.assertThat(structuredFormCommandHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleFinaliseStructuredForm")
                        .thatHandles(MATERIAL_COMMAND_FINALISE_STRUCTURED_FORM)));
    }

    private class StubEventStream implements EventStream {

        public Stream<JsonEnvelope> events;

        @Override
        public Stream<JsonEnvelope> read() {
            return null;
        }

        @Override
        public Stream<JsonEnvelope> readFrom(long l) {
            return null;
        }

        @Override
        public Stream<JsonEnvelope> readFrom(final long l, final int i) {
            return null;
        }

        @Override
        public long append(Stream<JsonEnvelope> events) throws EventStreamException {
            this.events = events;
            return 0L;
        }

        @Override
        public long append(Stream<JsonEnvelope> stream, Tolerance tolerance) throws EventStreamException {
            return 0;
        }

        @Override
        public long appendAfter(Stream<JsonEnvelope> stream, long l) throws EventStreamException {
            return 0;
        }

        @Override
        public long size() {
            return 0;
        }

        @Override
        public UUID getId() {
            return null;
        }

        @Override
        public long getPosition() {
            return 0;
        }

        @Override
        public String getName() {
            return null;
        }
    }
}
