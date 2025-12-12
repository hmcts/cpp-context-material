package uk.gov.moj.cpp.material.command.handler.client;

import uk.gov.justice.services.common.configuration.Value;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;

/**
 * Client for sending REST messages.
 */
public class RestClient {

    @Inject
    @Value(key = "alfresco.proxy.type", defaultValue = "none")
    public String proxyType;

    @Inject
    @Value(key = "alfresco.proxy.hostname", defaultValue = "none")
    public String proxyHostname;

    @Inject
    @Value(key = "alfresco.proxy.port", defaultValue = "0")
    public String proxyPort;

    /**
     * Sends a message via post.
     *
     * @param uri       - the URI to post the message to.
     * @param mediaType - the mediaType of the message.
     * @param headers   - any Http headers required.
     * @param entity    - the entity to post.
     * @return the response from the Http request.
     */
    public Response post(final String uri, final MediaType mediaType, final MultivaluedHashMap<String, Object> headers, final Entity entity) {
        return getClient()
                .target(uri)
                .request(mediaType)
                .headers(headers)
                .post(entity);
    }

    private Client getClient() {
        Client client;

        if ("none".equals(proxyType)) {
            client = ClientBuilder.newClient();
        } else {
            client = new ResteasyClientBuilderImpl()
                    .defaultProxy(proxyHostname, Integer.parseInt(proxyPort), proxyType)
                    .build();
        }
        return client;
    }

}
