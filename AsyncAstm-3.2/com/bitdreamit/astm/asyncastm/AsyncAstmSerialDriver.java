package com.bitdreamit.astm.asyncastm;

import com.bitdreamit.astm.asyncastm.service.connection.AbstractAstmConnection;
import com.bitdreamit.astm.asyncastm.service.connection.AstmSerialConnection;
import com.bitdreamit.astm.asyncastm.service.connection.Protocol;
import com.bitdreamit.astm.asyncastm.service.states.AstmStateMachine;
import com.bitdreamit.astm.asyncastm.service.states.bundle.AstmContext;
import com.bitdreamit.astm.asyncastm.service.states.bundle.ReceivedMessage;
import com.bitdreamit.astm.asyncastm.service.states.bundle.TransmissionResult;
import org.apache.log4j.Logger;

/**
 * Serial (RS-232) implementation of AsyncAstmDriver.
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

    private AstmContext context;
    private AstmStateMachine stateMachine;

    public void setPortName(String v) { this.portName = v; }
    public void setBaudRate(int v) { this.baudRate = v; }
    public void setDataBits(int v) { this.dataBits = v; }
    public void setStopBits(int v) { this.stopBits = v; }
    public void setParity(int v) { this.parity = v; }
    public void setFlowControl(int v) { this.flowControl = v; }
    public void setProtocol(String v) { this.protocol = v; }
    public void setCharset(String v) { this.charset = v; }

    @Override
    public void start() throws Exception {
        String protoUpper = protocol.trim().toUpperCase();
        Protocol p;
        try {
            p = Protocol.valueOf(protoUpper);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown protocol: '" + protocol + "'. Valid: ELECSYS, COBAS");
        }

        // FIX: Only validate baud rate and data bits.
        // Stop bits, parity, and flow control are already mapped to valid jSerialComm
        // constants by AstmService.java; validating them here causes crashes
        // (e.g. 2 stop bits = jSerialComm constant 3, which fails "< 1 || > 2").
        validateSerialParams();

        AbstractAstmConnection conn = new AstmSerialConnection(
                portName, baudRate, dataBits, stopBits, parity, flowControl, p, charset);
        this.context = new AstmContext(conn);
        this.stateMachine = new AstmStateMachine(context);
        stateMachine.start();
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
        // REMOVED: stopBits, parity, flowControl validation.
        // AstmService maps UI values to jSerialComm constants correctly.
    }

    @Override
    public void stop() throws Exception {
        if (stateMachine != null) {
            stateMachine.close();
        }
        logger.info("AsyncAstmSerialDriver stopped");
    }

    @Override
    public boolean send(byte[] data) throws Exception {
        TransmissionResult result = context.sendMessage(new String(data, charset));
        return result.getStatus() == TransmissionResult.Status.SUCCESS;
    }

    @Override
    public byte[] receive() throws Exception {
        if (context == null) throw new IllegalStateException("Driver not started");
        ReceivedMessage msg = context.getReceivedMessage();
        return msg != null ? msg.getMessage().getBytes(charset) : new byte[0];
    }

    @Override
    public boolean isConnected() {
        return stateMachine != null && stateMachine.getCurrentStatus() != null;
    }

    @Override
    public ReceivedMessage getReceivedMessage() throws InterruptedException {
        return context.getReceivedMessage();
    }

    @Override
    public TransmissionResult sendMessage(String message) throws InterruptedException {
        return context.sendMessage(message);
    }
}