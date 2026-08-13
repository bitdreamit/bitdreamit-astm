package com.bitdreamit.astm.asyncastm;

import com.bitdreamit.astm.asyncastm.service.connection.AbstractAstmConnection;
import com.bitdreamit.astm.asyncastm.service.connection.AstmSerialConnection;
import com.bitdreamit.astm.asyncastm.service.connection.Protocol;
import com.bitdreamit.astm.asyncastm.service.states.AstmStateMachine;
import com.bitdreamit.astm.asyncastm.service.states.bundle.AstmContext;
import com.bitdreamit.astm.asyncastm.service.states.bundle.ReceivedMessage;
import com.bitdreamit.astm.asyncastm.service.states.bundle.TransmissionResult;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmStatusCallback;
import org.apache.log4j.Logger;

/**
 * Serial (RS-232) implementation of AsyncAstmDriver.
 *
 * FIX (Bug #6): isConnected() was returning true based on `running` flag
 * (which is set at the end of start()) instead of actual state machine status.
 * Now matches the same logic as AsyncAstmTcpDriver — only true when state
 * machine has reached IDLE / SENDING / RECEIVING.
 *
 * FIX (Bug #4): Added setStatusCallback() to allow AstmReceiver / AstmDispatcher
 * to register a status callback after the driver is constructed.
 */
public class AsyncAstmSerialDriver implements AsyncAstmDriver {
    private static final Logger logger = Logger.getLogger(AsyncAstmSerialDriver.class);

    private String portName = "COM1";
    private int baudRate = 9600;
    private int dataBits = 8;
    private int stopBits = 1;
    private int parity = 0;
    private int flowControl = 0;
    private String protocol = "ELECSYS";
    private String charset = "windows-1252";

    private AstmStatusCallback callback;
    private AstmContext context;
    private AstmStateMachine stateMachine;
    private volatile boolean running = false;

    public void setPortName(String v) { this.portName = v; }
    public void setBaudRate(int v) { this.baudRate = v; }
    public void setDataBits(int v) { this.dataBits = v; }
    public void setStopBits(int v) { this.stopBits = v; }
    public void setParity(int v) { this.parity = v; }
    public void setFlowControl(int v) { this.flowControl = v; }
    public void setProtocol(String v) { this.protocol = v; }
    public void setCharset(String v) { this.charset = v; }

    /**
     * FIX (Bug #4): Allow AstmReceiver / AstmDispatcher to register a status
     * callback after the driver is constructed by AstmService.createDriver().
     */
    public void setStatusCallback(AstmStatusCallback callback) {
        this.callback = callback;
        if (stateMachine != null) {
            stateMachine.addCallback(callback);
        }
    }

    @Override
    public void start() throws Exception {
        String protoUpper = protocol.trim().toUpperCase();
        Protocol p;
        try {
            p = Protocol.valueOf(protoUpper);
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown protocol '" + protocol + "', defaulting to ELECSYS");
            p = Protocol.ELECSYS;
        }

        validateSerialParams();

        AbstractAstmConnection conn = new AstmSerialConnection(
                portName, baudRate, dataBits, stopBits, parity, flowControl, p, charset);
        this.context = new AstmContext(conn);
        this.stateMachine = new AstmStateMachine(context);
        if (callback != null) {
            stateMachine.addCallback(callback);
        }
        stateMachine.start();
        this.running = true;
        logger.info("AsyncAstmSerialDriver started on " + portName);
    }

    private void validateSerialParams() {
        int[] validBauds = {1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200};
        boolean baudOk = false;
        for (int b : validBauds) { if (b == baudRate) { baudOk = true; break; } }
        if (!baudOk) {
            throw new IllegalArgumentException("Invalid baud rate: " + baudRate);
        }
        if (dataBits < 5 || dataBits > 8) {
            throw new IllegalArgumentException("Invalid data bits: " + dataBits + ". Must be 5-8.");
        }
    }

    @Override
    public void stop() throws Exception {
        this.running = false;
        if (stateMachine != null) {
            stateMachine.close();
        }
        logger.info("AsyncAstmSerialDriver stopped");
    }

    @Override
    public boolean send(byte[] data) throws Exception {
        if (context == null) return false;
        TransmissionResult result = context.sendMessage(new String(data, charset));
        return result.getStatus() == TransmissionResult.Status.SUCCESS;
    }

    @Override
    public byte[] receive() throws Exception {
        return new byte[0];
    }

    /**
     * FIX (Bug #6): Match AsyncAstmTcpDriver behavior — return true only when
     * the state machine has reached an actually-connected state.
     */
    @Override
    public boolean isConnected() {
        if (stateMachine == null) return false;
        AstmConnectionStatus s = stateMachine.getCurrentStatus();
        return s == AstmConnectionStatus.IDLE
            || s == AstmConnectionStatus.SENDING
            || s == AstmConnectionStatus.RECEIVING;
    }

    @Override
    public ReceivedMessage getReceivedMessage() throws InterruptedException {
        if (context == null) throw new IllegalStateException("Driver not started");
        return context.getReceivedMessage();
    }

    @Override
    public TransmissionResult sendMessage(String message) throws InterruptedException {
        if (context == null) throw new IllegalStateException("Driver not started");
        return context.sendMessage(message);
    }
}
