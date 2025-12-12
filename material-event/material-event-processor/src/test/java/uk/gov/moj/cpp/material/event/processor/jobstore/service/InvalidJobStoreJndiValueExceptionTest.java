package uk.gov.moj.cpp.material.event.processor.jobstore.service;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class InvalidJobStoreJndiValueExceptionTest {

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        final String message = "message";
        final Throwable cause = new Throwable();

        final InvalidJobStoreJndiValueException exception = new InvalidJobStoreJndiValueException(message, cause);

        assertThat(exception.getMessage(), is(message));
        assertThat(exception.getCause(), is(cause));
    }
}