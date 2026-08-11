package com.bitdreamit.astm.asyncastm.service.connection;

import com.fazecast.jSerialComm.SerialPort;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketException;

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
        logger.info("Opening serial port: " + portName + " @ " + baudRate);
        serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(baudRate);
        serialPort.setNumDataBits(dataBits);
        serialPort.setNumStopBits(stopBits);
        serialPort.setParity(parity);
        serialPort.setFlowControl(flowControl);
        // Default 5s timeout until IdleState calls setSocketTimeout(0) for indefinite blocking
        serialPort.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 5000, 5000);
        if (!serialPort.openPort()) {
            throw new RuntimeException("Failed to open serial port: " + portName);
        }
        logger.info("Serial port opened successfully");
        this.initialize();  // FIX #2: Start the background reader thread
    }

    @Override
    protected OutputStream doGetOutputStream() throws IOException {
        return serialPort.getOutputStream();
    }

    @Override
    protected InputStream doGetInputStream() throws IOException {
        return serialPort.getInputStream();
    }

    @Override
    public InetSocketAddress getAddress() {
        return new InetSocketAddress(portName, 0);
    }

    @Override
    public void setSocketTimeout(int seconds) throws SocketException {
        // FIX: Dynamically update serial port read timeout.
        // seconds=0 -> block indefinitely (IdleState waiting for ENQ)
        // seconds=15/30 -> block up to that many seconds (active transfer)
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
    }
}