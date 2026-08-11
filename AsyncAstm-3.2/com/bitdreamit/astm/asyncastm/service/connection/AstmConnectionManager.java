package com.bitdreamit.astm.asyncastm.service.connection;

/**
 * High-level manager for ASTM transport (TCP or Serial).
 * Implemented by AsyncAstmTcpDriver and AsyncAstmSerialDriver.
 */
public interface AstmConnectionManager {
    void start() throws Exception;
    void stop() throws Exception;
    boolean send(byte[] data) throws Exception;
    byte[] receive() throws Exception;
    boolean isConnected();
    void setConnectionListener(AstmConnectionListener listener);
}
