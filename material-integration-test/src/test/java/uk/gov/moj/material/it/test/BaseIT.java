package uk.gov.moj.material.it.test;


import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;

import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@ExtendWith(JmsResourceManagementExtension.class)
@Execution(ExecutionMode.CONCURRENT)
public class BaseIT {

    public static final String CONTEXT_NAME = "material";
    public static final String WIREMOCK_RESET = "WIREMOCK_RESET";

    @BeforeAll
    public static void setupOnce() {
        configureFor(System.getProperty("INTEGRATION_HOST_KEY", "localhost"), 8080);
        if (System.getProperty(WIREMOCK_RESET, "true").equalsIgnoreCase("true")) {
            reset();
        }
    }

}