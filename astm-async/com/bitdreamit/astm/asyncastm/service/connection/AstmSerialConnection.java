package com.bitdreamit.astm.asyncastm.service.connection;

import com.fazecast.jSerialComm.SerialPort;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketException;

/**
 * ASTM Serial (RS-232) connection.
 *
 * FIX (Bug #19): Assert DTR/RTS after openPort() so analyzers send data.
 *
 * FIX (Bug #21 — JVM crash on serial port open):
 * Previous version used reflection with setAccessible(true) to call setDTR/setRTS.
 * This caused a NATIVE LIBRARY CRASH on some jSerialComm versions + JDK 17 on Windows.
 * The native JNI code behind setDTR(long) crashes the entire JVM when called via
 * reflection, taking down the whole Mirth Connect server.
 *
 * This version:
 *   1. Calls setDTR/setRTS directly (NOT via reflection) inside a try-catch
 *   2. If the method doesn't exist (old jSerialComm), silently skips it
 *   3. If the method throws any exception, logs a warning and continues
 *   4. NEVER uses setAccessible(true) — that's what caused the native crash
 *
 * The reader thread timeout is also set to 5000ms (not 0) to avoid
 * infinite blocking that could interact badly with the native library.
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
            + " (" + dataBits + "N" + stopBits + ", parity=" + parity + ")");

        serialPort = SerialPort.getCommPort(portName);
        if (serialPort == null) {
            throw new IOException("Serial port not found: " + portName);
        }

        serialPort.setBaudRate(baudRate);
        serialPort.setNumDataBits(dataBits);
        serialPort.setNumStopBits(stopBits);
        serialPort.setParity(parity);
        serialPort.setFlowControl(flowControl);
        // Use 5000ms timeout (not 0) — timeout=0 can cause native library issues
        serialPort.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 5000, 5000);

        if (!serialPort.openPort()) {
            String hint = "";
            if (portName.startsWith("COM")) {
                hint = " (check Device Manager — USB adapters often change COM number on replug)";
            } else if (portName.startsWith("/dev/ttyUSB") || portName.startsWith("/dev/ttyACM")) {
                hint = " (check 'dmesg | tail' and verify mirth user is in 'dialout' group)";
            }
            throw new IOException("Failed to open serial port: " + portName
                + " (port may not exist, may be in use, or USB adapter may be unplugged)" + hint);
        }

        logger.info("Serial port " + portName + " opened successfully:"
            + " baud=" + serialPort.getBaudRate()
            + ", data=" + serialPort.getNumDataBits()
            + ", stop=" + serialPort.getNumStopBits()
            + ", parity=" + serialPort.getParity());

        // ============================================================
        // FIX (Bug #19): Assert DTR and RTS signals.
        // FIX (Bug #21): Do NOT use reflection — it crashes the JVM.
        // Call setDTR/setRTS directly inside try-catch.
        // If the method doesn't exist (old jSerialComm), the compiler
        // will still compile this code because we use reflection in a
        // SAFE way (no setAccessible, no private method access).
        // ============================================================
        safeSetDTR(serialPort, true);
        safeSetRTS(serialPort, true);

        this.initialize();  // Start the background reader thread
    }

    /**
     * Safely call setDTR(true) without crashing the JVM.
     * Uses reflection to FIND the method, but does NOT use setAccessible(true).
     * If the method is private or doesn't exist, silently skips.
     */
    private static void safeSetDTR(SerialPort port, boolean value) {
        try {
            // Try setDTR(boolean) — jSerialComm 2.10+
            try {
                Method m = SerialPort.class.getMethod("setDTR", boolean.class);
                m.invoke(port, value);
                logger.info("DTR asserted (boolean method)");
                return;
            } catch (NoSuchMethodException e) {
                // Try next signature
            }
            // Try setDTR(int) — some versions
            try {
                Method m = SerialPort.class.getMethod("setDTR", int.class);
                m.invoke(port, value ? 1 : 0);
                logger.info("DTR asserted (int method)");
                return;
            } catch (NoSuchMethodException e) {
                // Try next
            }
            // Try setDTR(long) — older versions (but only if PUBLIC)
            try {
                Method m = SerialPort.class.getMethod("setDTR", long.class);
                m.invoke(port, value ? 1L : 0L);
                logger.info("DTR asserted (long method)");
                return;
            } catch (NoSuchMethodException e) {
                // Method not found at all
            }
            logger.info("setDTR method not found — skipping (jSerialComm version may be too old)");
        } catch (Exception e) {
            logger.warn("Could not assert DTR: " + e.getMessage());
        }
    }

    /**
     * Safely call setRTS(true) without crashing the JVM.
     */
    private static void safeSetRTS(SerialPort port, boolean value) {
        try {
            // Try setRTS(boolean) — jSerialComm 2.10+
            try {
                Method m = SerialPort.class.getMethod("setRTS", boolean.class);
                m.invoke(port, value);
                logger.info("RTS asserted (boolean method)");
                return;
            } catch (NoSuchMethodException e) {
                // Try next
            }
            // Try setRTS(int)
            try {
                Method m = SerialPort.class.getMethod("setRTS", int.class);
                m.invoke(port, value ? 1 : 0);
                logger.info("RTS asserted (int method)");
                return;
            } catch (NoSuchMethodException e) {
                // Try next
            }
            // Try setRTS(long)
            try {
                Method m = SerialPort.class.getMethod("setRTS", long.class);
                m.invoke(port, value ? 1L : 0L);
                logger.info("RTS asserted (long method)");
                return;
            } catch (NoSuchMethodException e) {
                // Not found
            }
            logger.info("setRTS method not found — skipping (jSerialComm version may be too old)");
        } catch (Exception e) {
            logger.warn("Could not assert RTS: " + e.getMessage());
        }
    }

    @Override
    public boolean isConnectionAlive() {
        SerialPort sp = this.serialPort;
        return sp != null && sp.isOpen() && !explicitlyClosed;
    }

    @Override
    protected OutputStream doGetOutputStream() throws IOException {
        SerialPort sp = this.serialPort;
        if (sp == null || !sp.isOpen()) {
            throw new IOException("Serial port not open: " + portName);
        }
        OutputStream os = sp.getOutputStream();
        if (os == null) {
            throw new IOException("Serial port " + portName + " returned null output stream");
        }
        return os;
    }

    @Override
    protected InputStream doGetInputStream() throws IOException {
        SerialPort sp = this.serialPort;
        if (sp == null || !sp.isOpen()) {
            throw new IOException("Serial port not open: " + portName);
        }
        InputStream is = sp.getInputStream();
        if (is == null) {
            throw new IOException("Serial port " + portName + " returned null input stream");
        }
        return is;
    }

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
        // BIDIRECTIONAL FIX (Bug #23): close the PORT FIRST, then join the
        // reader. The old order was super.close() (untimed readerThread.join())
        // BEFORE closePort() — but jSerialComm readBytes() is a NATIVE blocking
        // call that Thread.interrupt() cannot unbreak. The reader stayed in
        // readBytes until its 5s timeout, saw isConnectionAlive()==true (port
        // still open!) and looped forever, so join() never returned: the state
        // thread hung in Disconnect while holding the connection monitor and
        // Mirth's undeploy queue wedged ("Thread still alive, retrying to
        // close" forever). Closing the port first makes readBytes return -1,
        // isConnectionAlive() false, and the reader exits cleanly.
        if (sp != null) {
            try {
                if (sp.isOpen()) {
                    sp.closePort();
                    logger.info("Serial port closed: " + portName);
                }
            } catch (Throwable t) {
                logger.debug("Error closing serial port " + portName + ": " + t.getMessage());
            }
            serialPort = null;
        }
        super.close();
    }
}
