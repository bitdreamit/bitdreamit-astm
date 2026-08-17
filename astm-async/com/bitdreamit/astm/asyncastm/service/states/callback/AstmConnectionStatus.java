package com.bitdreamit.astm.asyncastm.service.states.callback;

public enum AstmConnectionStatus {
    STARTING,
    CONNECTING,
    IDLE,
    SENDING,
    RECEIVING,
    RECONNECTING,
    DISCONNECTING,
    EXITING,
    ERROR
}
