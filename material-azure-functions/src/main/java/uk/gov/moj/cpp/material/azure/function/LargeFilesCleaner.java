package uk.gov.moj.cpp.material.azure.function;

import static java.lang.String.format;
import static java.lang.System.getenv;
import static java.util.Optional.ofNullable;

import uk.gov.moj.cpp.material.azure.exception.AzureBlobClientException;
import uk.gov.moj.cpp.material.azure.storage.CleanLargeFileStore;
import uk.gov.moj.cpp.material.azure.storage.LargeFileAzureBlobClientService;

import java.time.LocalDateTime;
import java.util.logging.Level;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;

public class LargeFilesCleaner {

    private static final String CONNECTION_STRING = ofNullable(getenv("material.alfrescoAzureStorageConnectionString"))
            .orElse("");

    @FunctionName("purgeLargeFiles")
    public void run(
            @TimerTrigger(name = "timerInfo", schedule = "00:30:00")
            final String timerInfo,
            final ExecutionContext context) {
        context.getLogger().info(() -> format("purgeLargeFiles function executed at: %s", LocalDateTime.now()));
        context.getLogger().info(() -> format( "CONNECTION_STRING = '%s'", CONNECTION_STRING));
        context.getLogger().info(() -> format( "ContainerName = '%s'",  getenv("material.alfrescoAzureStorageContainerName")));

        try {
            final LargeFileAzureBlobClientService largeFileAzureBlobClientService = new LargeFileAzureBlobClientService();
            largeFileAzureBlobClientService.connect(CONNECTION_STRING);
            final CleanLargeFileStore cleanLargeFileStore = new CleanLargeFileStore(largeFileAzureBlobClientService.getContainer());
            cleanLargeFileStore.clean(context);

            context.getLogger().info("purgeLargeFiles function completed");
        } catch (AzureBlobClientException e) {
            context.getLogger().log(Level.SEVERE, "Could not clean up material large files", e);
        }
    }
}
