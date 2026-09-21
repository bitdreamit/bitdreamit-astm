package com.bitdreamit.connect.astm;

import com.mirth.connect.plugins.ServerPlugin;
import com.bitdreamit.astm.asyncastm.AsyncAstmDriver;
import com.bitdreamit.astm.asyncastm.AsyncAstmSerialDriver;
import com.bitdreamit.astm.asyncastm.AsyncAstmTcpDriver;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmStatusCallback;
import com.fazecast.jSerialComm.SerialPort;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AstmService — factory + lifecycle wrapper for AsyncAstmDriver.
 *
 * This class is used in two ways:
 *
 *  1. Plugin-level: declared in plugin.xml <serverClasses>. Mirth instantiates
 *     one instance per server. Calls start() and stop(). This instance's
 *     `driver` field is always null — start()/stop() are no-ops except for
 *     the static whitelist initialization.
 *
 *  2. Per-channel: AstmReceiver.onStart() / AstmDispatcher.onStart() create
 *     their own AstmService instance via `new AstmService()`, call
 *     init(properties) [or init(properties, callback)] to create a driver,
 *     then call startDriver() / stopDriver() to manage its lifecycle.
 *
 * FIX (Bug #7): The static initializer that calls AstmWhitelist.whiteListClasses()
 * was missing in v3.0.2 — restored here. Without it, Mirth 4.x's strict XStream
 * security whitelist rejects AstmReceiverProperties / AstmDispatcherProperties
 * during channel XML deserialization, and channels fail to deploy with a
 * ForbiddenClassException.
 *
 * FIX (Bug #4): Added init(AstmProperties, AstmStatusCallback) overload so
 * that AstmReceiver / AstmDispatcher can pass a status callback that the
 * driver will use to report connection state changes back to Mirth's
 * EventController. Without this callback, the Mirth dashboard connector
 * status badge stays blank — the "channel not enable" symptom.
 */
public class AstmService implements ServerPlugin {
    private static final Logger logger = Logger.getLogger(AstmService.class);

    // Per-channel driver instance (null at plugin level)
    private AsyncAstmDriver driver;

    // =========================================================================
    // BIDIRECTIONAL FIX (A4) — shared per-channel connection registry.
    //
    // The old design created TWO independent drivers per channel: one inside
    // AstmReceiver (source) and one inside AstmDispatcher (destination).
    // Consequences:
    //   - TCP_SERVER: both tried to LISTEN on the same port -> BindException,
    //     or if configured on different ports the instrument's query arrived
    //     on the source socket and the order was sent on a socket the
    //     instrument never opened. Bidirectional query->answer was impossible.
    //   - SERIAL: two drivers cannot open the same COM port.
    //   - TCP_CLIENT: two separate sockets to the analyzer.
    //
    // With the registry, AstmReceiver registers its driver under the channel
    // id and AstmDispatcher REUSES it. Both directions then share ONE state
    // machine / ONE socket: when the instrument's Host Query (TSREQ / Q record)
    // arrives, the channel transformer answers through the destination and the
    // order goes out on the SAME connection (IdleState sends ENQ right after
    // the instrument's query transfer completes) — exactly the Pentra 400 6.1
    // -> 6.2, i-800 TSREQ^REAL -> TSDWN^REAL and D-10 query sequences.
    // =========================================================================
    private static final Map<String, AsyncAstmDriver> SHARED_DRIVERS = new ConcurrentHashMap<>();

    public static void registerSharedDriver(String channelId, AsyncAstmDriver driver) {
        if (channelId != null && driver != null) {
            SHARED_DRIVERS.put(channelId, driver);
            logger.info("ASTM shared driver registered for channel " + channelId);
        }
    }

    public static AsyncAstmDriver findSharedDriver(String channelId) {
        return (channelId == null) ? null : SHARED_DRIVERS.get(channelId);
    }

    public static void unregisterSharedDriver(String channelId) {
        if (channelId != null && SHARED_DRIVERS.remove(channelId) != null) {
            logger.info("ASTM shared driver unregistered for channel " + channelId);
        }
    }

    /**
     * BIDIRECTIONAL FIX (A4): create a lightweight AstmService wrapper around
     * an already-running shared driver (used by AstmDispatcher in shared mode).
     */
    public static AstmService wrapSharedDriver(AsyncAstmDriver sharedDriver) {
        AstmService service = new AstmService();
        service.driver = sharedDriver;
        return service;
    }

    // FIX (Bug #7): Restore the static whitelist initializer. Without this,
    // Mirth 4.x's XStream security policy rejects AstmReceiverProperties and
    // AstmDispatcherProperties during channel XML deserialization. The symptom
    // is channels failing to deploy with a ForbiddenClassException.
    static {
        try {
            AstmWhitelist.whiteListClasses();
        } catch (Throwable t) {
            // Whitelist registration is best-effort — if Mirth is too old to
            // support allowTypes(), we silently skip (matches v2.4.2 behavior).
            logger.warn("Failed to register ASTM XStream whitelist: " + t.getMessage());
        }
    }

    public AstmService() {
    }

    /**
     * Per-channel init: create driver from properties, no status callback.
     * Equivalent to calling init(props, null).
     */
    public void init(AstmProperties props) {
        init(props, null);
    }

    /**
     * FIX (Bug #4): Per-channel init with status callback. The callback will
     * be registered with the driver's state machine so that connection state
     * changes (CONNECTING, IDLE, RECEIVING, RECONNECTING, EXITING) are reported
     * back to Mirth's EventController via ConnectionStatusEvent dispatches.
     *
     * Without this callback, the Mirth dashboard connector status badge stays
     * blank — the "channel not enable" / "no status shown" symptom.
     */
    public void init(AstmProperties props, AstmStatusCallback callback) {
        this.driver = createDriver(props, callback);
    }

    private AsyncAstmDriver createDriver(AstmProperties props, AstmStatusCallback callback) {
        switch (props.getTransportMode()) {
            case SERIAL:
                AsyncAstmSerialDriver serialDriver = new AsyncAstmSerialDriver();
                serialDriver.setPortName(props.getSerialPort());
                serialDriver.setBaudRate(props.getBaudRate());
                serialDriver.setDataBits(props.getDataBits());

                int stopBits;
                switch (props.getStopBits()) {
                    case 1: stopBits = SerialPort.ONE_STOP_BIT; break;
                    case 2: stopBits = SerialPort.TWO_STOP_BITS; break;
                    default: stopBits = SerialPort.ONE_STOP_BIT; break;
                }
                serialDriver.setStopBits(stopBits);

                int parity;
                switch (props.getParity()) {
                    case 0: parity = SerialPort.NO_PARITY; break;
                    case 1: parity = SerialPort.ODD_PARITY; break;
                    case 2: parity = SerialPort.EVEN_PARITY; break;
                    default: parity = SerialPort.NO_PARITY; break;
                }
                serialDriver.setParity(parity);

                int flowControl;
                switch (props.getFlowControl()) {
                    case 0: flowControl = SerialPort.FLOW_CONTROL_DISABLED; break;
                    case 1: flowControl = SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED; break;
                    case 2: flowControl = SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED | SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED; break;
                    default: flowControl = SerialPort.FLOW_CONTROL_DISABLED; break;
                }
                serialDriver.setFlowControl(flowControl);

                serialDriver.setProtocol(props.getAstmProtocol());
                serialDriver.setCharset(props.getCharsetName());
                // BIDIRECTIONAL FIX (A1/A2): propagate framing configuration
                serialDriver.setFrameConfig(props.getMaxFrameSize(), props.isUseChecksum());
                if (callback != null) {
                    serialDriver.setStatusCallback(callback);
                }
                return serialDriver;

            case TCP_SERVER:
                AsyncAstmTcpDriver serverDriver = new AsyncAstmTcpDriver(
                    props.getPort(), true, props.getAstmProtocol());
                serverDriver.setCharset(props.getCharsetName());
                // BIDIRECTIONAL FIX (A1/A2): propagate framing configuration
                serverDriver.setFrameConfig(props.getMaxFrameSize(), props.isUseChecksum());
                if (callback != null) {
                    serverDriver.setStatusCallback(callback);
                }
                return serverDriver;

            case TCP_CLIENT:
            default:
                AsyncAstmTcpDriver clientDriver = new AsyncAstmTcpDriver(
                    props.getHost(), props.getPort(), false, props.getAstmProtocol());
                clientDriver.setCharset(props.getCharsetName());
                // BIDIRECTIONAL FIX (A1/A2): propagate framing configuration
                clientDriver.setFrameConfig(props.getMaxFrameSize(), props.isUseChecksum());
                if (callback != null) {
                    clientDriver.setStatusCallback(callback);
                }
                return clientDriver;
        }
    }

    // ========== ServerPlugin lifecycle (plugin-level instance) ==========

    @Override
    public void start() {
        logger.info("AstmService plugin started (v3.0.3 patched)");
    }

    @Override
    public void stop() {
        // Per-channel drivers are stopped by AstmReceiver.onStop() / AstmDispatcher.onStop().
        // At plugin level, driver is null.
        try {
            if (driver != null) {
                driver.stop();
            }
        } catch (Exception e) {
            logger.error("Error stopping ASTM service", e);
        }
    }

    @Override
    public String getPluginPointName() {
        return "ASTM Settings";
    }

    // ========== Per-channel driver lifecycle ==========

    public void startDriver() throws Exception {
        if (driver == null) throw new IllegalStateException("Driver not initialized. Call init() first.");
        driver.start();
    }

    public void stopDriver() throws Exception {
        if (driver != null) {
            driver.stop();
        }
    }

    public AsyncAstmDriver getDriver() {
        return driver;
    }

    public boolean send(byte[] data) throws Exception {
        if (driver == null) throw new IllegalStateException("Driver not initialized");
        return driver.send(data);
    }
}
