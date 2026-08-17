package com.bitdreamit.astm.asyncastm.service.connection.file;

/**
 * Iterates over ASTM message frames.
 */
public interface MessageIterator {
    boolean hasNext();
    String nextFrame();
}
