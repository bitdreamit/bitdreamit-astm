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

    /**
     * Force-close the underlying connection. The state machine will then
     * naturally transition to ReconnectState and retry the connection using
     * the latest properties (so a COM port name change in the channel
     * settings takes effect on the next reconnect).
     *
     * Used by the simple EOFException retry loop in IdleState when the
     * analyzer drops without sending EOT.
     */
    void forceReconnect();
}
