package com.wb.lock;

import java.util.concurrent.TimeUnit;

public interface WbLock {

    public void lock();

    public boolean tryLock();

    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    public void lockInterruptibly() throws InterruptedException;

    public void unlock();
}
