package uk.gov.moj.cpp.material.query.api.interceptors;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CacheInterceptorTest {

    final private String queryName = "material.query";
    @InjectMocks
    private CacheInterceptor cacheInterceptor;

    private JsonObject jParams;

    @Test
    public void shouldTranslateQueryAPIWithoutQueryParams() {
        final String cacheKey = cacheInterceptor.translateQueryApiToKey(queryName, createObjectBuilder().build());
        assertThat(cacheKey, is(queryName));
    }

    @Test
    public void shouldTranslateQueryAPIWithOneQueryParams() {
        jParams = createObjectBuilder()
                .add("materialId", "1234567")
                .build();
        final String cacheKey = cacheInterceptor.translateQueryApiToKey(queryName, jParams);
        assertThat(cacheKey, is("material.query?materialId=\"1234567\""));
    }

}
