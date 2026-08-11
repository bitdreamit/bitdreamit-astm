package com.bitdreamit.astm.asyncastm.service.states.bundle;

public class ReceivedMessage {
    private String message;
    private TransmissionResult result;

    public ReceivedMessage(String message, TransmissionResult result) {
        this.message = message;
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public TransmissionResult getResult() {
        return result;
    }

    public void setResult(TransmissionResult result) {
        this.result = result;
    }
}
