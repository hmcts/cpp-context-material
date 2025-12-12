package uk.gov.moj.cpp.material.event.processor.jobstore.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileUploadRetryConfigurationTest {


    @Test
    void shouldReturnRetryDurations() {
        final FileUploadRetryConfiguration fileUploadRetryConfiguration = new FileUploadRetryConfiguration();
        ReflectionUtil.setField(fileUploadRetryConfiguration, "alfrescoFileUploadTaskRetryDurationsSeconds", "30 ,  60 ");

        final List<Long> retryDurationsSeconds = fileUploadRetryConfiguration.getAlfrescoFileUploadTaskRetryDurationsSeconds();

        assertThat(retryDurationsSeconds.size(), is(2));
        assertThat(retryDurationsSeconds.get(0), is(30L));
        assertThat(retryDurationsSeconds.get(1), is(60L));
    }

    @Test
    void shouldThrowException_whenConfigurationValueIsNotParsable() {
        final FileUploadRetryConfiguration fileUploadRetryConfiguration = new FileUploadRetryConfiguration();
        ReflectionUtil.setField(fileUploadRetryConfiguration, "alfrescoFileUploadTaskRetryDurationsSeconds", "20, abc");

        final InvalidJobStoreJndiValueException e = assertThrows(InvalidJobStoreJndiValueException.class,
                fileUploadRetryConfiguration::getAlfrescoFileUploadTaskRetryDurationsSeconds);

        assertThat(e.getMessage(), is("Failed to parse '20, abc' value configured through JNDI parameter, name: material.task.alfresco-file-upload.retry.threshold.durations.seconds"));
    }
}