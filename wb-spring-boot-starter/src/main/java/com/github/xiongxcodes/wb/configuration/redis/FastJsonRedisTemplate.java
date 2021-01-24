package com.github.xiongxcodes.wb.configuration.redis;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

public class FastJsonRedisTemplate extends RedisTemplate<Object, Object> {
    public FastJsonRedisTemplate(RedisConnectionFactory connectionFactory) {
        FastJson2JsonRedisSerializer<Object> keyserializer = new FastJson2JsonRedisSerializer<Object>(Object.class);
        FastJson2JsonRedisSerializer<Object> valueserializer = new FastJson2JsonRedisSerializer<Object>(Object.class);
        this.setConnectionFactory(connectionFactory);
        this.setKeySerializer(keyserializer);
        this.setValueSerializer(valueserializer);
        this.afterPropertiesSet();
    }
}