package com.bitdreamit.astm.asyncastm.service.states.bundle;

public class TransmissionResult {
    private Status status;
    private String description;

    public TransmissionResult(Status status, String description) {
        this.status = status;
        this.description = description;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static enum Status {
        SUCCESS,
        TIMEOUT,
        DISCONNECTED,
        REJECTED,
        INTERRUPTED,
        UNKNOWN
    }
}
