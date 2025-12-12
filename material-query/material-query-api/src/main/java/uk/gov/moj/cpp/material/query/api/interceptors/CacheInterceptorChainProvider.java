package uk.gov.moj.cpp.material.query.api.interceptors;

import static uk.gov.justice.services.core.annotation.Component.QUERY_API;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.core.interceptor.InterceptorChainEntry;
import uk.gov.justice.services.core.interceptor.InterceptorChainEntryProvider;
import uk.gov.service.CacheService;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheInterceptorChainProvider implements InterceptorChainEntryProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheInterceptorChainProvider.class);
    private final List<InterceptorChainEntry> interceptorChainEntries = new LinkedList<>();
    @Inject
    @Value(key = "redisCommonCacheEnabled", defaultValue = "true")
    private String redisCommonCacheEnabled;

    @Inject
    private CacheService cacheService;

    @PostConstruct
    public void createInterceptorChainEntries() {

        LOGGER.info("CacheInterceptorChainProvider: Start ");
        if (Boolean.parseBoolean(redisCommonCacheEnabled)) {
            LOGGER.info("Redis Cache Enabled !!! ");
            interceptorChainEntries.add(new InterceptorChainEntry(5010, CacheInterceptor.class));
        }
        LOGGER.info("CacheInterceptorChainProvider: End ");
    }

    @Override
    public String component() {
        return QUERY_API;
    }

    @Override
    public List<InterceptorChainEntry> interceptorChainTypes() {
        return new ArrayList<>(interceptorChainEntries);
    }
}


