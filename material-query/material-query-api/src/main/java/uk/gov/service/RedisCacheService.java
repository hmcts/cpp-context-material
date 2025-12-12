package uk.gov.service;

import uk.gov.justice.services.common.configuration.Value;

import java.time.Duration;

import javax.inject.Inject;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SetArgs;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisCacheService implements CacheService {

    private static final Duration CONNECT_TIMEOUT_ONE_SEC = Duration.ofSeconds(1);
    private static final String LOCALHOST = "localhost";
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisCacheService.class);
    @Inject
    @Value(key = "redisCommonCacheHost", defaultValue = "localhost")
    private String host;

    @Inject
    @Value(key = "redisCommonCacheKey", defaultValue = "none")
    private String key;

    @Inject
    @Value(key = "redisCommonCachePort", defaultValue = "6381")
    private String port;

    @Inject
    @Value(key = "redisCommonCacheUseSsl", defaultValue = "false")
    private String useSsl;

    @Inject
    @Value(key = "redisCommonCacheKeyTTL", defaultValue = "86400")
    private String ttlSeconds;

    private RedisClient redisClient = null;

    @Override
    public String add(final String key, final String value) {
        if (LOCALHOST.equals(host)) {
            return null;
        }
        if (redisClient == null) {
            LOGGER.info("Inside CacheService.add() - redisClient not initailised; invoking setRedisClient");
            setRedisClient();
        }
        return executeAddCommand(key, value);
    }

    private void setRedisClient() {
        LOGGER.info("Redis host : {}", host);
        final String keyPart = ("none".equals(this.key) ? "" : this.key + "@");
        final RedisURI redisURI = RedisURI.create("redis://" + keyPart + host + ":" + port + "/2");
        redisURI.setSsl(Boolean.parseBoolean(useSsl));

        redisClient = RedisClient.create(redisURI);

        final SocketOptions socketOptions =
                SocketOptions.builder()
                        .connectTimeout(CONNECT_TIMEOUT_ONE_SEC).build();

        final ClientOptions clientOptions =
                ClientOptions.builder().socketOptions(socketOptions).build();
        this.redisClient.setOptions(clientOptions);
    }

    @Override
    public String get(final String key) {
        if (LOCALHOST.equals(host)) {
            return null;
        }
        if (redisClient == null) {
            LOGGER.info("Inside CacheService.get() - redisClient not initialised; invoking setRedisClient");
            setRedisClient();
        }
        return executeGetCommand(key);
    }

    private String executeAddCommand(final String key, final String value) {

        try (final StatefulRedisConnection<String, String> connection = this.redisClient.connect()) {
            final RedisCommands<String, String> command = connection.sync();
            final SetArgs args = new SetArgs().ex(Integer.parseInt(ttlSeconds));
            return command.set(key, value, args);

        } catch (RedisConnectionException ex) {
            LOGGER.warn("Exception in RedisCache executeAddCommand() - {} ", ex);
            return null;
        }
    }

    private String executeGetCommand(final String key) {
        try (final StatefulRedisConnection<String, String> connection = this.redisClient.connect()) {
            final RedisCommands<String, String> command = connection.sync();
            return command.get(key);
        } catch (RedisConnectionException ex) {
            LOGGER.warn("Exception in RedisCache executeGetCommand() - {} ", ex);
            return null;
        }
    }
}
