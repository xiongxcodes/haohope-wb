package com.github.xiongxcodes.wb.configuration.redis;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import com.wb.lock.DictionaryLock;
import com.wb.lock.DistributedLock;

public class RedissonDictionaryLock implements DictionaryLock, DistributedLock {
    private RLock rLock;

    public RedissonDictionaryLock(RedissonClient redisson) {
        rLock = new RedissonLock(redisson).getLock(key());
    }

    @Override
    public String key() {
        return "dictionary";
    }

    @Override
    public void lock() {
        rLock.lock();
    }

    @Override
    public boolean tryLock() {
        return rLock.tryLock();
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return rLock.tryLock(time, unit);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        rLock.lockInterruptibly();
    }

    @Override
    public void unlock() {
        rLock.unlock();
    }
}
