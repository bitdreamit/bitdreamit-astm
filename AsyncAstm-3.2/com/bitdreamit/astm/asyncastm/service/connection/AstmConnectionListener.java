package com.bitdreamit.astm.asyncastm.service.connection;

/**
 * Callback interface for ASTM connection events.
 */
public interface AstmConnectionListener {
    void onConnected();
    void onDisconnected();
    void onDataReceived(byte[] data);
    void onError(Exception error);
}
