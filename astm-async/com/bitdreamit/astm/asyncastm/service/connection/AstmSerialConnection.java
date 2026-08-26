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
 * FIX (Bug #19 — Some analyzers don't send data via Serial even though PuTTY does):
 *
 * Root cause: jSerialComm's openPort() does NOT assert DTR (Data Terminal Ready)
 * or RTS (Request To Send) signals. Many lab analyzers check these signals
 * before transmitting.
 *
 * PuTTY asserts DTR/RTS by default. jSerialComm does NOT.
 *
 * IMPLEMENTATION NOTE: Different jSerialComm versions have different method
 * signatures for setDTR/setRTS:
 *   - v2.10+: setDTR(boolean), setRTS(boolean) — public
 *   - v2.9 and earlier: setDTR(long), setRTS(long) — may be private
 *   - v1.x: may not have these methods at all
 *
 * This class uses reflection to call setDTR/setRTS with whatever parameter
 * type the installed jSerialComm version provides. This makes the code
 * compatible with ALL jSerialComm versions without compile errors.
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
            + " (" + dataBits + "N" + stopBits + ", parity=" + parity
            + ", flowControl=" + flowControl + ")");
        serialPort = SerialPort.getCommPort(portName);
        if (serialPort == null) {
            throw new IOException("Serial port not found: " + portName);
        }
        serialPort.setBaudRate(baudRate);
        serialPort.setNumDataBits(dataBits);
        serialPort.setNumStopBits(stopBits);
        serialPort.setParity(parity);
        serialPort.setFlowControl(flowControl);

        // FIX (Bug #19): Use timeout=0 (block indefinitely) instead of 5000ms.
        // With TIMEOUT_READ_BLOCKING and timeout=0, read() blocks until data arrives
        // or the port is closed. This prevents false EOF on slow analyzers.
        serialPort.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 5000);

        if (!serialPort.openPort()) {
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

        // ============================================================
        // FIX (Bug #19 — THE KEY FIX): Assert DTR and RTS signals.
        // ============================================================
        // Many lab analyzers will NOT send any data until DTR and RTS
        // are asserted (set to HIGH/true). jSerialComm's openPort()
        // does NOT do this automatically.
        //
        // Use reflection because different jSerialComm versions have
        // different method signatures:
        //   - v2.10+: public setDTR(boolean)
        //   - v2.9-:  private setDTR(long) 
        //   - v1.x:   no setDTR method
        // ============================================================
        boolean dtrOk = setSerialPortSignal(serialPort, "setDTR", true);
        boolean rtsOk = setSerialPortSignal(serialPort, "setRTS", true);

        // Log the actual serial port configuration for debugging
        logger.info("Serial port " + portName + " opened successfully:"
            + " baud=" + serialPort.getBaudRate()
            + ", data=" + serialPort.getNumDataBits()
            + ", stop=" + serialPort.getNumStopBits()
            + ", parity=" + serialPort.getParity()
            + ", flow=" + serialPort.getFlowControlSettings()
            + ", DTR_asserted=" + dtrOk
            + ", RTS_asserted=" + rtsOk);

        if (!dtrOk) {
            logger.warn("Could not assert DTR signal on " + portName
                + " — some analyzers may not send data. Check jSerialComm version.");
        }
        if (!rtsOk) {
            logger.warn("Could not assert RTS signal on " + portName
                + " — some analyzers may not send data. Check jSerialComm version.");
        }

        this.initialize();  // Start the background reader thread — port is now open
    }

    /**
     * FIX (Bug #19): Use reflection to call setDTR/setRTS on the SerialPort.
     * Handles all jSerialComm versions:
     *   - public setDTR(boolean) — jSerialComm 2.10+
     *   - private setDTR(long)   — jSerialComm 2.9 and earlier
     *   - no setDTR method       — jSerialComm 1.x
     *
     * @param port     the SerialPort instance
     * @param methodName  "setDTR" or "setRTS"
     * @param value    true = assert (HIGH), false = de-assert (LOW)
     * @return true if the method was found and called successfully, false otherwise
     */
    private static boolean setSerialPortSignal(SerialPort port, String methodName, boolean value) {
        // Try all possible parameter types: boolean, int, long
        Class<?>[] paramTypes = {boolean.class, int.class, long.class};
        for (Class<?> paramType : paramTypes) {
            try {
                Method m = SerialPort.class.getDeclaredMethod(methodName, paramType);
                m.setAccessible(true); // allow access to private methods
                Object arg;
                if (paramType == boolean.class) {
                    arg = value;
                } else if (paramType == int.class) {
                    arg = value ? 1 : 0;
                } else {
                    arg = value ? 1L : 0L;
                }
                m.invoke(port, arg);
                return true;
            } catch (NoSuchMethodException e) {
                // Try next parameter type
                continue;
            } catch (Exception e) {
                logger.warn("Error calling " + methodName + "(" + paramType.getSimpleName()
                    + " = " + value + "): " + e.getMessage());
                return false;
            }
        }
        // Method not found with any parameter type
        logger.warn(methodName + " method not found in jSerialComm.SerialPort — "
            + "signal will not be asserted. jSerialComm version may be too old.");
        return false;
    }

    /**
     * FIX (Bug #19): Check if the serial port is still open.
     */
    @Override
    public boolean isConnectionAlive() {
        SerialPort sp = this.serialPort;
        return sp != null && sp.isOpen() && !explicitlyClosed;
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
     * available for I/O.
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
                    // Lower DTR/RTS before closing (clean handshake teardown)
                    setSerialPortSignal(sp, "setDTR", false);
                    setSerialPortSignal(sp, "setRTS", false);
                    sp.closePort();
                    logger.info("Serial port closed: " + portName);
                }
            } catch (Throwable t) {
                logger.debug("Error closing serial port " + portName + " (may already be gone): "
                    + t.getMessage());
            }
            serialPort = null;
        }
    }
}
