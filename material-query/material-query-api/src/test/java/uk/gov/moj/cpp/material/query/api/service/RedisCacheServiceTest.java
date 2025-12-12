package uk.gov.moj.cpp.material.query.api.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.service.RedisCacheService;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RedisCacheServiceTest {

    @InjectMocks
    private RedisCacheService redisCacheService;

    @Mock
    private RedisClient redisClient;

    @Mock
    private StatefulRedisConnection statefulRedisConnection;

    @Mock
    private RedisCommands<String, String> redisCommands;

    private void mockRedis() {
        setField(redisCacheService, "redisClient", redisClient);
        setField(redisCacheService, "host", "redisHost");
        setField(redisCacheService, "key", "test_key");
        setField(redisCacheService, "port", "6380");
        setField(redisCacheService, "useSsl", "false");
        setField(redisCacheService, "ttlSeconds", "86400");

        when(redisClient.connect()).thenReturn(statefulRedisConnection);
        when(statefulRedisConnection.sync()).thenReturn(redisCommands);
    }

    @BeforeEach
    public void setUp() {
        redisCacheService = new RedisCacheService();
        mockRedis();
    }

    @Test
    public void shouldAddToCacheSuccessfully() {
        when(redisCommands.set(eq("key1"), eq("value1"), any())).thenReturn("value1");
        assertThat(redisCacheService.add("key1", "value1"), is("value1"));
    }

    @Test
    public void shouldGetFromCacheSuccessfully() {
        when(redisCommands.get("key1")).thenReturn("value1");
        assertThat(redisCacheService.get("key1"), is("value1"));
    }
}
