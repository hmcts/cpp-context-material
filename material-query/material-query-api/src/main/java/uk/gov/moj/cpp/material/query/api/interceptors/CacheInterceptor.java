package uk.gov.moj.cpp.material.query.api.interceptors;

import static java.lang.String.format;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.interceptor.Interceptor;
import uk.gov.justice.services.core.interceptor.InterceptorChain;
import uk.gov.justice.services.core.interceptor.InterceptorContext;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.service.CacheService;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheInterceptor implements Interceptor {
    private static final String NULL_VALUE = "null";
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheInterceptor.class);

    final List<String> nonCachedQueryEndpoints = Arrays.asList(
            "material.query.is-downloadable-materials",
            "material.query.structured-form",
            "material.query.structured-form-change-history",
            "material.query.structured-form-defendant-user");

    @Inject
    private CacheService cacheService;

    @Inject
    private StringToJsonObjectConverter converter;

    @Override
    public InterceptorContext process(final InterceptorContext interceptorContext, final InterceptorChain interceptorChain) {
        final String queryName = interceptorContext.inputEnvelope().metadata().name();
        final JsonObject jsonParams = interceptorContext.inputEnvelope().payloadAsJsonObject();

        final boolean ignoreRedisCache = nonCachedQueryEndpoints.contains(queryName);

        final String cacheKey = translateQueryApiToKey(queryName, jsonParams);
        LOGGER.debug("CacheInterceptor: CacheKey : {} ", cacheKey);

        final JsonEnvelope envelope;
        final String cacheValue = cacheService.get(cacheKey);

        if (ignoreRedisCache) {
            LOGGER.info("Not cached in Redis {}  ", queryName);
            return interceptorChain.processNext(interceptorContext);
        } else if (cacheValue != null) {
            LOGGER.debug("Cache Key Found !!! ");

            final JsonObject jsonObject = converter.convert(cacheValue);
            envelope = envelopeFrom(interceptorContext.inputEnvelope().metadata(), jsonObject);

            return interceptorContext.copyWithOutput(envelope);
        } else {
            LOGGER.debug("Cache Key Not Found !!! ");
            final InterceptorContext responseContext = interceptorChain.processNext(interceptorContext);

            responseContext.outputEnvelope()
                    .ifPresent(e -> {
                        if (e.payload() != null && StringUtils.isNotEmpty(e.payload().toString()) && !StringUtils.equals(e.payload().toString(), NULL_VALUE)) {
                            cacheService.add(cacheKey, e.payload().toString());
                        }
                    });
            return responseContext;
        }
    }

    String translateQueryApiToKey(final String queryName, final JsonObject jsonParams) {

        final StringBuilder cacheKey = new StringBuilder(queryName);
        final Set<String> keys = jsonParams.keySet();

        if (CollectionUtils.isNotEmpty(keys)) {
            final String queryString = keys
                    .stream()
                    .map(key -> format("%s=%s", key, jsonParams.get(key).toString()))
                    .collect(Collectors.joining("&"));

            cacheKey.append("?").append(queryString);
        }
        return cacheKey.toString();
    }
}

