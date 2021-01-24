package com.github.xiongxcodes.wb.configuration.stat;

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import com.github.xiongxcodes.wb.configuration.properties.WbStatProperties;
import com.github.xiongxcodes.wb.configuration.redis.FastJsonRedisTemplate;
import com.github.xiongxcodes.wb.configuration.redis.RedisData;
import com.github.xiongxcodes.wb.configuration.redis.RedisKVCache;
import com.github.xiongxcodes.wb.configuration.redis.RedisUrlCache;
import com.github.xiongxcodes.wb.configuration.redis.RedisVarCache;
import com.github.xiongxcodes.wb.configuration.redis.RedissonDictionaryLock;
import com.github.xiongxcodes.wb.configuration.redis.RedissonKVLock;
import com.github.xiongxcodes.wb.configuration.redis.RedissonModuleLock;
import com.github.xiongxcodes.wb.configuration.redis.RedissonUrlLock;
import com.github.xiongxcodes.wb.configuration.redis.RedissonVarLock;
import com.github.xiongxcodes.wb.configuration.redis.ReidsDictionaryCache;
import com.github.xiongxcodes.wb.configuration.redis.ReidsModuleCache;
import com.wb.common.Dictionary;
import com.wb.common.KVBuffer;
import com.wb.common.UrlBuffer;
import com.wb.common.UserList;
import com.wb.common.Var;
import com.wb.interact.Module;

@EnableConfigurationProperties(WbStatProperties.class)
@ConditionalOnBean(RedisConnectionFactory.class)
@ConditionalOnProperty(prefix = "gg", name = "away", havingValue = "redis")
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class WbRedisConfiguration {
    @Bean
    public FastJsonRedisTemplate fastJsonRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new FastJsonRedisTemplate(connectionFactory);
    }

    @Bean
    public RedisVarCache redisVarCache(FastJsonRedisTemplate fastJsonRedisTemplate) {
        return new RedisVarCache(fastJsonRedisTemplate);
    }

    @Bean
    public Var var() {
        return new Var();
    }

    @Bean
    public Module module() {
        return new Module();
    }

    @Bean
    public RedisKVCache redisKVCache(FastJsonRedisTemplate fastJsonRedisTemplate) {
        return new RedisKVCache(fastJsonRedisTemplate);
    }

    @Bean
    public KVBuffer kvBuffer() {
        return new KVBuffer();
    }

    @Bean
    public RedisUrlCache redisUrlCache(FastJsonRedisTemplate fastJsonRedisTemplate) {
        return new RedisUrlCache(fastJsonRedisTemplate);
    }

    @Bean
    public UrlBuffer urlBuffer() {
        return new UrlBuffer();
    }

    @Bean
    public ReidsModuleCache reidsModuleCache(FastJsonRedisTemplate fastJsonRedisTemplate) {
        return new ReidsModuleCache(fastJsonRedisTemplate);
    }

    @Bean
    public ReidsDictionaryCache reidsDictionaryCache(FastJsonRedisTemplate fastJsonRedisTemplate) {
        return new ReidsDictionaryCache(fastJsonRedisTemplate);
    }

    @Bean
    public Dictionary dictionary() {
        return new Dictionary();
    }

    @Bean
    public UserList userList() {
        return new UserList();
    }

    @Bean
    public RedisData redisData() {
        return new RedisData();
    }

    @Bean
    public RedissonKVLock redissonKVLock(RedissonClient redisson) {
        return new RedissonKVLock(redisson);
    }

    @Bean
    public RedissonUrlLock redissonUrlLock(RedissonClient redisson) {
        return new RedissonUrlLock(redisson);
    }

    @Bean
    public RedissonVarLock redissonVarLock(RedissonClient redisson) {
        return new RedissonVarLock(redisson);
    }

    @Bean
    public RedissonModuleLock redissonModuleLock(RedissonClient redisson) {
        return new RedissonModuleLock(redisson);
    }

    @Bean
    public RedissonDictionaryLock redissonDictionaryLock(RedissonClient redisson) {
        return new RedissonDictionaryLock(redisson);
    }
}
