package com.bitdreamit.astm.asyncastm.service.connection;

public enum Protocol {
    ELECSYS,
    COBAS;

    public static Protocol[] all() {
        return Protocol.values();
    }
}
