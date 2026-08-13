package com.bitdreamit.astm.asyncastm;

import com.bitdreamit.astm.asyncastm.service.states.bundle.ReceivedMessage;
import com.bitdreamit.astm.asyncastm.service.states.bundle.TransmissionResult;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmStatusCallback;

import java.util.concurrent.TimeUnit;

/**
 * Common interface for both TCP and Serial ASTM drivers.
 */
public interface AsyncAstmDriver {
    void start() throws Exception;
    void stop() throws Exception;
    boolean send(byte[] data) throws Exception;
    byte[] receive() throws Exception;
    boolean isConnected();

    /**
     * Blocks until a message arrives. USE WITH CARE: if the driver's state
     * machine has died, this will block forever because nothing will ever
     * put a message into the incoming queue.
     *
     * Prefer {@link #pollReceivedMessage(long, TimeUnit)} in listener loops
     * so the caller can periodically check {@link #isAlive()} and bail out
     * when the driver is dead.
     */
    ReceivedMessage getReceivedMessage() throws InterruptedException;

    /**
     * FIX: Polls for a received message with a timeout. Returns null if no
     * message arrived within the timeout. This allows the listener thread
     * to periodically check whether the underlying driver is still alive
     * and exit gracefully instead of blocking forever after a driver death.
     */
    ReceivedMessage pollReceivedMessage(long timeout, TimeUnit unit) throws InterruptedException;

    TransmissionResult sendMessage(String message) throws InterruptedException;

    /**
     * Register a status callback that receives state-machine lifecycle events
     * (CONNECTING, IDLE, RECEIVING, SENDING, RECONNECTING, EXITING, ERROR, ...).
     *
     * The callback is the ONLY way the driver talks back to Mirth. Without it,
     * the channel appears "silently enabled" because nothing dispatches
     * ConnectionStatusEvent / ErrorEvent to Mirth's EventController.
     *
     * Must be called BEFORE start().
     */
    void addCallback(AstmStatusCallback callback);

    /**
     * True if the background state-machine thread is still alive.
     * Used by AstmReceiverService to detect that the driver has died
     * (so it can stop blocking on getReceivedMessage and log the failure).
     */
    boolean isAlive();
}
