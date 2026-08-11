package com.bitdreamit.astm.asyncastm;

import com.bitdreamit.astm.asyncastm.service.states.bundle.ReceivedMessage;
import com.bitdreamit.astm.asyncastm.service.states.bundle.TransmissionResult;

/**
 * Common interface for both TCP and Serial ASTM drivers.
 */
public interface AsyncAstmDriver {
    void start() throws Exception;
    void stop() throws Exception;
    boolean send(byte[] data) throws Exception;
    byte[] receive() throws Exception;
    boolean isConnected();
    ReceivedMessage getReceivedMessage() throws InterruptedException;
    TransmissionResult sendMessage(String message) throws InterruptedException;
}
