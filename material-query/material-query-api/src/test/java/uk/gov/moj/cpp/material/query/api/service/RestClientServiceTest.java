package uk.gov.moj.cpp.material.query.api.service;

import static com.google.common.net.MediaType.ZIP;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.service.RestClientService;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RestClientServiceTest {

    @Mock
    private Client client;
    @Mock
    private WebTarget target;
    @Mock
    private Invocation.Builder builder;
    @Mock
    private Response response;

    @InjectMocks
    private RestClientService restClientService;

    @Test
    public void shouldPerformHttpGetRequestWithCorrectParams() throws Exception {
        final String url = "http://localhost/";
        final String fromDate = "2018-01-01";
        final String toDate = "2018-02-02";

        when(client.target(url)).thenReturn(target);
        when(target.queryParam(anyString(), anyString())).thenReturn(target);
        when(target.request()).thenReturn(builder);
        when(builder.accept(ZIP.toString())).thenReturn(builder);
        when(builder.get()).thenReturn(response);

        restClientService.get(url, fromDate, toDate);

        verify(client).target(url);
        verify(target).queryParam("fromDate", fromDate);
        verify(target).queryParam("toDate", toDate);
    }

    @Test
    public void shouldReturnsResponseWithSameProperties() throws Exception {
        final byte[] bytes = new byte[0];
        when(response.readEntity(byte[].class)).thenReturn(bytes);
        when(response.getStatusInfo()).thenReturn(NOT_FOUND);
        when(response.getMediaType()).thenReturn(MediaType.valueOf("application/zip"));
        when(response.getHeaderString(CONTENT_DISPOSITION)).thenReturn("attachment; filename=\"application/zip\"");

        final Response newResponse = restClientService.newResponseFrom(response);

        assertThat(newResponse.getStatusInfo(), is(NOT_FOUND));
        assertThat(newResponse.getHeaderString(CONTENT_DISPOSITION), is("attachment; filename=\"application/zip\""));
        assertThat(newResponse.getEntity(), is(bytes));
        assertThat(newResponse.getHeaderString(CONTENT_TYPE), is("application/zip"));
    }

}
