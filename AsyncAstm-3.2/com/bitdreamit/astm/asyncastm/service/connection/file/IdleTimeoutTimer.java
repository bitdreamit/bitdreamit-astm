package com.bitdreamit.astm.asyncastm.service.connection.file;

/**
 * Simple timer for idle state timeouts.
 */
public class IdleTimeoutTimer {
    private long startTime;
    private long timeoutMs;

    public IdleTimeoutTimer() {
        this.startTime = System.currentTimeMillis();
        this.timeoutMs = 0;
    }

    public void reset() {
        this.startTime = System.currentTimeMillis();
        this.timeoutMs = 0;
    }

    public void reset(int seconds) {
        this.startTime = System.currentTimeMillis();
        this.timeoutMs = seconds * 1000L;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - this.startTime >= this.timeoutMs;
    }
}
