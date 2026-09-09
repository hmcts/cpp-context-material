package uk.gov.moj.material.it.test;


import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@ExtendWith(JmsResourceManagementExtension.class)
@Execution(ExecutionMode.CONCURRENT)
public class BaseIT {

    public static final String CONTEXT_NAME = "material";

    @RegisterExtension
    public static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

}