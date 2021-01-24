package com.github.xiongxcodes.wb.configuration.redis;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

public class RedissonLock {
    private final String NAMESPACE = "haohope:lock:wb";
    private RedissonClient redisson;

    public RedissonLock(RedissonClient redisson) {
        this.redisson = redisson;
    }

    public RLock getLock(String key) {
        return redisson.getLock(NAMESPACE + ":" + key);
    }

    public String key() {
        return null;
    }
}
