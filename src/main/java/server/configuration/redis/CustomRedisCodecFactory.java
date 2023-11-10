package server.configuration.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.codec.RedisCodec;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.runtime.context.scope.ThreadLocal;
import jakarta.inject.Singleton;
import server.common.dto.Motion;

@Factory
public class CustomRedisCodecFactory {

    @Singleton
    @ThreadLocal
    @Replaces(factory = RedisCodec.class)
    public RedisCodec<String, Motion> customRedisCodec(ObjectMapper objectMapper) {
        return new JacksonRedisCodecMotion(objectMapper);
    }
}
