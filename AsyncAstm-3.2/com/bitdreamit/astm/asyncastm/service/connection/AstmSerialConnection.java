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
 *
 * FIX (Bug #15 — USB-COM hang / unplug recovery):
 * Added isOpen() health check + improved logging for USB-COM unplug scenarios.
 * When a USB-RS232 adapter is unplugged, jSerialComm's InputStream.read()
 * typically returns -1 (EOF), which propagates through the AbstractAstmConnection
 * reader thread as an EOFException, which ReconnectState then handles with
 * backoff. The combination of:
 *
 *   - 5-second read timeout (setComPortTimeouts)
 *   - EOF detection in the reader thread
 *   - ReconnectState with exponential backoff (5s -> 60s)
 *   - WARN-level logging on every reconnect attempt
 *
 * ...produces the following behavior for USB-COM unplug:
 *
 *   t=0s    USB cable unplugged
 *   t<=5s   read() returns -1 (read timeout reached OR OS detected unplug)
 *   t<=5s   Reader thread sets lastByte=-1, releases readySemaphore, exits
 *   t<=5s   IdleState's readByteImpl() sees lastByte=-1, throws EOFException
 *   t<=5s   AstmState.run() catches EOFException, transitions to ReconnectState
 *   t=5s    ReconnectState logs WARN: "Connection lost — will retry in 5s (attempt #1)"
 *   t=10s   ReconnectState closes port, transitions to ConnectState
 *   t=10s   ConnectState calls doConnect() -> openPort() fails (USB still out)
 *   t=10s   ReconnectState logs WARN: "Reconnect attempt #2 — waiting 10s"
 *   t=20s   ... (10s backoff)
 *   t=30s   ... attempt #3, waiting 20s
 *   ...
 *   t=N     User plugs USB back in
 *   t=N+5s  openPort() succeeds -> IdleState -> ready to receive LIS data
 *
 * The attempt counter resets on successful connect (see ConnectState), so the
 * next failure starts backoff fresh from 5s.
 */
public class AstmSerialConnection extends AbstractAstmConnection {
    private static final Logger logger = Logger.getLogger(AstmSerialConnection.class);

    private String portName;
    private int baudRate;
    private int dataBits;
    private int stopBits;
    private int parity;
    private int flowControl;
    private volatile SerialPort serialPort;
    private volatile boolean explicitlyClosed = false;

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
        explicitlyClosed = false;
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
        // Default 5s timeout — this is what makes read() return -1 when USB is unplugged,
        // rather than blocking forever. The 5s is also what bounds the time before
        // ReconnectState gets a chance to run.
        serialPort.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 5000, 5000);
        if (!serialPort.openPort()) {
            // FIX (Bug #9): throw IOException, not RuntimeException
            // FIX (Bug #15): helpful message for the most common USB-COM causes
            String hint = "";
            if (portName.startsWith("COM")) {
                hint = " (on Windows, check Device Manager to confirm the COM port number "
                    + "matches — USB adapters often enumerate as a different COM number on replug)";
            } else if (portName.startsWith("/dev/ttyUSB") || portName.startsWith("/dev/ttyACM")) {
                hint = " (on Linux, check 'dmesg | tail' to see if the USB adapter was re-enumerated "
                    + "with a different name, and verify the mirth user is in the 'dialout' group)";
            } else if (portName.startsWith("/dev/ttyS")) {
                hint = " (on Linux, /dev/ttyS* are native UART ports — for USB adapters use /dev/ttyUSB*)";
            }
            throw new IOException("Failed to open serial port: " + portName
                + " (port may not exist, may be in use by another process, or USB adapter may be unplugged)"
                + hint);
        }
        logger.info("Serial port " + portName + " opened successfully");
        this.initialize();  // Start the background reader thread — port is now open
    }

    @Override
    protected OutputStream doGetOutputStream() throws IOException {
        SerialPort sp = this.serialPort;
        if (sp == null || !sp.isOpen()) {
            throw new IOException("Serial port not open: " + portName
                + " (may have been unplugged)");
        }
        OutputStream os = sp.getOutputStream();
        if (os == null) {
            throw new IOException("Serial port " + portName + " returned null output stream "
                + "(port may have been disconnected)");
        }
        return os;
    }

    @Override
    protected InputStream doGetInputStream() throws IOException {
        SerialPort sp = this.serialPort;
        if (sp == null || !sp.isOpen()) {
            throw new IOException("Serial port not open: " + portName
                + " (may have been unplugged)");
        }
        InputStream is = sp.getInputStream();
        if (is == null) {
            throw new IOException("Serial port " + portName + " returned null input stream "
                + "(port may have been disconnected)");
        }
        return is;
    }

    /**
     * Health check: returns true if the serial port is currently open and
     * available for I/O. Used by external watchdogs to detect USB-COM hang.
     */
    public boolean isOpen() {
        SerialPort sp = this.serialPort;
        return sp != null && sp.isOpen() && !explicitlyClosed;
    }

    @Override
    public InetSocketAddress getAddress() {
        return new InetSocketAddress(portName, 0);
    }

    @Override
    public void setSocketTimeout(int seconds) throws SocketException {
        SerialPort sp = this.serialPort;
        if (sp != null && sp.isOpen()) {
            int timeoutMs = seconds * 1000;
            sp.setComPortTimeouts(
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
        explicitlyClosed = true;
        SerialPort sp = this.serialPort;
        super.close();
        if (sp != null) {
            try {
                if (sp.isOpen()) {
                    sp.closePort();
                    logger.info("Serial port closed: " + portName);
                }
            } catch (Throwable t) {
                // jSerialComm may throw if the USB was already unplugged — log and continue.
                logger.debug("Error closing serial port " + portName + " (may already be gone): "
                    + t.getMessage());
            }
            serialPort = null;
        }
    }
}
