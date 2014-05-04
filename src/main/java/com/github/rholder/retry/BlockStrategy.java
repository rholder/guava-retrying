package com.github.rholder.retry;

public interface BlockStrategy {

    void block(long sleepTime) throws InterruptedException;

}