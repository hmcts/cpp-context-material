package uk.gov.moj.cpp.material.event.processor.jobstore.service;

import java.util.Arrays;
import java.util.List;
import uk.gov.justice.services.common.configuration.GlobalValue;

import javax.inject.Inject;

public class FileUploadRetryConfiguration {

    private static final int THIRTY_SECONDS = 30;
    private static final int SIXTY_SECONDS = 60;
    private static final int THREE_MINUTES = 180;
    private static final int FIVE_MINUTES = 300;
    private static final int THIRTY_MINUTES = 1_800;
    private static final int ONE_HOUR = 3_600;
    private static final int TWO_HOURS = 7_200;
    private static final int FOUR_HOURS = 14_400;
    private static final int EIGHT_HOURS = 28_800;
    private static final int ONE_DAY = 86_400;
    private static final int TWO_DAYS = 172_800;
    private static final int ONE_WEEK = 604_800;
    private static final int ONE_YEAR = 31_536_000;

    private static final String RETRY_DURATIONS_SECONDS =
                    THIRTY_SECONDS + ", " +
                    SIXTY_SECONDS + ", " +
                    THREE_MINUTES + ", " +
                    FIVE_MINUTES + ", " +
                    THIRTY_MINUTES + ", " +
                    ONE_HOUR + ", " +
                    TWO_HOURS + ", " +
                    FOUR_HOURS + ", " +
                    EIGHT_HOURS + ", " +
                    ONE_DAY + ", " +
                    TWO_DAYS + ", " +
                    ONE_WEEK + ", " +
                    ONE_YEAR + ", " +
                    ONE_YEAR + ", " +
                    ONE_YEAR;


    @Inject
    @GlobalValue(key = "material.task.alfresco-file-upload.retry.threshold.durations.seconds", defaultValue = RETRY_DURATIONS_SECONDS)
    private String alfrescoFileUploadTaskRetryDurationsSeconds;

    public List<Long> getAlfrescoFileUploadTaskRetryDurationsSeconds() {
        try{
            return Arrays.stream(alfrescoFileUploadTaskRetryDurationsSeconds.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .toList();
        } catch (NumberFormatException nfe) {
            throw new InvalidJobStoreJndiValueException(String.format("""
                    Failed to parse '%s' value configured through JNDI parameter, \
                    name: material.task.alfresco-file-upload.retry.threshold.durations.seconds""", alfrescoFileUploadTaskRetryDurationsSeconds), nfe);
        }
    }
}
