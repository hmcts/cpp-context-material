package uk.gov.service;

import static com.google.common.net.MediaType.ZIP;
import static java.lang.Integer.parseInt;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;

import uk.gov.justice.services.common.configuration.Value;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RestClientService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestClientService.class);

    @Inject
    @Value(key = "restClient.httpConnection.poolSize", defaultValue = "10")
    private String httpConnectionPoolSize;

    @Inject
    @Value(key = "restClient.httpConnection.timeout", defaultValue = "120000")//2minutes
    private String httpConnectionTimeout;

    private Client client;

    @PostConstruct
    public void init() {
        final int poolSize = parseInt(httpConnectionPoolSize);
        final int timeout = parseInt(httpConnectionTimeout);

        client = new ResteasyClientBuilderImpl()
                .connectionPoolSize(poolSize)
                .maxPooledPerRoute(poolSize)
                .connectionTTL(timeout, TimeUnit.MILLISECONDS)
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .connectionCheckoutTimeout(timeout, TimeUnit.MILLISECONDS)
                .build();
    }


    @PreDestroy
    public void preDestroy() {
        client.close();
    }


    public Response get(final String url, final String fromDate, final String toDate) {
        return client
                .target(url)
                .queryParam("fromDate", fromDate)
                .queryParam("toDate", toDate)
                .request()
                .accept(ZIP.toString())
                .get();
    }

    public Response newResponseFrom(final Response oldResponse) {


        try {
            // read entity no matter what. Could be json err message or zip content. All fine.
            final byte[] entity = oldResponse.readEntity(byte[].class);
            return Response.status(oldResponse.getStatusInfo())
                    .entity(entity)
                    .type(oldResponse.getMediaType())
                    .header(CONTENT_DISPOSITION, oldResponse.getHeaderString(CONTENT_DISPOSITION))
                    .build();

        } finally {
            try {
                oldResponse.close();
            } catch (RuntimeException e) {
                    LOGGER.debug("Failed to close old response ", e);
                }

        }
    }

}
