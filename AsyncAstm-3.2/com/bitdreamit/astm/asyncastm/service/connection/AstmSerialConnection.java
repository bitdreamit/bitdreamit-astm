package com.bitdreamit.astm.asyncastm.service.connection;

import com.fazecast.jSerialComm.SerialPort;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketException;

/**
 * ASTM Serial (RS-232) connection.
 *
 * FIX (Bug #9): doConnect() previously threw RuntimeException("Failed to open
 * serial port: ..."). RuntimeException is not caught by AstmState.run() and
 * propagated all the way up to AstmStateMachine.stateLoop's catch(Exception),
 * which logged at FATAL and killed the state machine. Now throws IOException
 * which is caught by the state machine's normal error handling and triggers
 * a transition to ReconnectState.
 */
public class AstmSerialConnection extends AbstractAstmConnection {
    private static final Logger logger = Logger.getLogger(AstmSerialConnection.class);

    private String portName;
    private int baudRate;
    private int dataBits;
    private int stopBits;
    private int parity;
    private int flowControl;
    private SerialPort serialPort;

    public AstmSerialConnection(String portName, int baudRate, int dataBits, int stopBits,
                                int parity, int flowControl, Protocol protocol, String charset) {
        super(protocol, charset);
        this.portName = portName;
        this.baudRate = baudRate;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
        this.flowControl = flowControl;
    }

    @Override
    public void doConnect() throws IOException, InterruptedException {
        logger.info("Opening serial port: " + portName + " @ " + baudRate
            + " (" + dataBits + " data bits, " + stopBits + " stop bits, parity=" + parity + ")");
        serialPort = SerialPort.getCommPort(portName);
        if (serialPort == null) {
            // FIX (Bug #9): throw IOException, not RuntimeException
            throw new IOException("Serial port not found: " + portName);
        }
        serialPort.setBaudRate(baudRate);
        serialPort.setNumDataBits(dataBits);
        serialPort.setNumStopBits(stopBits);
        serialPort.setParity(parity);
        serialPort.setFlowControl(flowControl);
        // Default 5s timeout until IdleState calls setSocketTimeout(0) for indefinite blocking
        serialPort.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 5000, 5000);
        if (!serialPort.openPort()) {
            // FIX (Bug #9): throw IOException, not RuntimeException
            throw new IOException("Failed to open serial port: " + portName
                + " (port may not exist, may be in use by another process, or may require elevated permissions)");
        }
        logger.info("Serial port " + portName + " opened successfully");
        this.initialize();  // Start the background reader thread — port is now open
    }

    @Override
    protected OutputStream doGetOutputStream() throws IOException {
        if (serialPort == null) {
            throw new IOException("Serial port not open");
        }
        return serialPort.getOutputStream();
    }

    @Override
    protected InputStream doGetInputStream() throws IOException {
        if (serialPort == null) {
            throw new IOException("Serial port not open");
        }
        return serialPort.getInputStream();
    }

    @Override
    public InetSocketAddress getAddress() {
        return new InetSocketAddress(portName, 0);
    }

    @Override
    public void setSocketTimeout(int seconds) throws SocketException {
        if (serialPort != null && serialPort.isOpen()) {
            int timeoutMs = seconds * 1000;
            serialPort.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
                    timeoutMs, 5000);
            logger.debug("Serial read timeout set to " + timeoutMs + " ms");
        }
    }

    @Override
    public boolean isServer() {
        return false;
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            logger.info("Serial port closed: " + portName);
        }
        serialPort = null;
    }
}
