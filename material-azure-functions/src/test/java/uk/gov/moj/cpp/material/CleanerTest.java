package uk.gov.moj.cpp.material;

import uk.gov.moj.cpp.material.azure.storage.LargeFileAzureBlobClientService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S2187")
public class CleanerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CleanerTest.class.getName());

    public static String CONNECTION_STRING = "DefaultEndpointsProtocol=https;AccountName=sastelargefiles;AccountKey=dummy;EndpointSuffix=core.windows.net";

    public static void main(String[] args) {
        try {
            final LargeFileAzureBlobClientService largeFileAzureBlobClientService = new LargeFileAzureBlobClientService();
            largeFileAzureBlobClientService.connect(CONNECTION_STRING);
            Cleaner cleaner = new Cleaner(largeFileAzureBlobClientService.getContainer());
            cleaner.clean();

        } catch (Exception ex) {
            LOGGER.error("Exception" + ex);
        }
    }
}
