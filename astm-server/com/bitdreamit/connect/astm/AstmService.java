package com.bitdreamit.connect.astm;

import com.mirth.connect.donkey.model.channel.DeployedState;
import com.mirth.connect.donkey.model.event.ConnectionStatusEventType;
import com.mirth.connect.donkey.server.StopException;
import com.mirth.connect.donkey.server.channel.Connector;
import com.mirth.connect.donkey.server.channel.DestinationConnector;
import com.mirth.connect.donkey.server.channel.SourceConnector;
import com.mirth.connect.donkey.server.event.ConnectionStatusEvent;
import com.mirth.connect.server.controllers.ControllerFactory;
import com.mirth.connect.server.controllers.EventController;
import com.mirth.connect.plugins.ServerPlugin;
import com.bitdreamit.astm.asyncastm.AsyncAstmDriver;
import com.bitdreamit.astm.asyncastm.AsyncAstmSerialDriver;
import com.bitdreamit.astm.asyncastm.AsyncAstmTcpDriver;
import com.bitdreamit.astm.asyncastm.service.connection.Protocol;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmStatusCallback;
import com.fazecast.jSerialComm.SerialPort;
import org.apache.log4j.Logger;

/**
 * Server-side service that owns the AsyncAstmDriver lifecycle.
 *
 * Implements {@link ServerPlugin} because the plugin.xml registers
 * "com.bitdreamit.connect.astm.AstmService" as a serverClass — Mirth will
 * instantiate it and call start()/stop() at plugin deploy/undeploy time.
 * The actual per-channel driver lifecycle is owned by
 * {@link AstmReceiver#onStart()} / {@link AstmDispatcher#onStart()} which
 * delegate to this class.
 *
 * FIX (channel-silent-start bug): This class now builds an AstmStatusCallback
 * that translates state-machine events into Mirth {@link ConnectionStatusEvent}s
 * and dispatches them through {@link EventController}. On EXITING / ERROR, it
 * calls {@code connector.getChannel().stop()} so Mirth actually marks the
 * channel as STOPPED instead of leaving it silently in STARTED state with
 * nothing listening.
 *
 * Previously the driver was constructed with NO callback, so state transitions
 * (including fatal errors) were completely invisible to Mirth. The channel
 * would appear "started" but no listener was active — exactly the symptom
 * reported by users ("channel not enabled, no log error, silently enabled
 * but no active").
 */
public class AstmService implements ServerPlugin {
    private static final Logger logger = Logger.getLogger(AstmService.class);

    private AsyncAstmDriver driver;
    private AstmProperties properties;
    private Connector connector;
    private EventController eventController;

    public AstmService() {
        this.eventController = ControllerFactory.getFactory().createEventController();
    }

    /**
     * FIX: Optional Connector reference allows the service to build a callback
     * that can call connector.getChannel().stop() on EXITING.
     */
    public void init(AstmProperties props, Connector connector) {
        this.properties = props;
        this.connector = connector;
        this.driver = createDriver(props);
        // FIX: register the callback so state-machine events reach Mirth.
        this.driver.addCallback(buildCallback());
    }

    /** Backward-compatible init() for callers that don't have a Connector reference. */
    public void init(AstmProperties props) {
        init(props, null);
    }

    private AsyncAstmDriver createDriver(AstmProperties props) {
        // Validate protocol string EARLY so a bad value (e.g. "GENERIC") produces
        // a clear error message before construction instead of a cryptic
        // IllegalArgumentException deep in the driver constructor.
        String protoStr = props.getAstmProtocol();
        if (protoStr == null || protoStr.trim().isEmpty()) {
            throw new IllegalArgumentException("ASTM protocol is null or empty. "
                    + "Set it to ELECSYS or COBAS in the channel source properties.");
        }
        String protoUpper = protoStr.trim().toUpperCase();
        try {
            Protocol.valueOf(protoUpper);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown ASTM protocol: '" + protoStr
                    + "'. Valid values: ELECSYS, COBAS.", e);
        }

        switch (props.getTransportMode()) {
            case SERIAL:
                return createSerialDriver(props);
            case TCP_SERVER:
                AsyncAstmTcpDriver serverDriver = new AsyncAstmTcpDriver(props.getPort(), true, protoUpper);
                serverDriver.setCharset(props.getCharsetName());
                return serverDriver;
            case TCP_CLIENT:
            default:
                AsyncAstmTcpDriver clientDriver = new AsyncAstmTcpDriver(props.getHost(), props.getPort(), false, protoUpper);
                clientDriver.setCharset(props.getCharsetName());
                return clientDriver;
        }
    }

    private AsyncAstmDriver createSerialDriver(AstmProperties props) {
        AsyncAstmSerialDriver driver = new AsyncAstmSerialDriver();

        driver.setPortName(props.getSerialPort());
        driver.setBaudRate(props.getBaudRate());
        driver.setDataBits(props.getDataBits());

        int stopBits;
        switch (props.getStopBits()) {
            case 1: stopBits = SerialPort.ONE_STOP_BIT; break;
            case 2: stopBits = SerialPort.TWO_STOP_BITS; break;
            default: stopBits = SerialPort.ONE_STOP_BIT; break;
        }
        driver.setStopBits(stopBits);

        int parity;
        switch (props.getParity()) {
            case 0: parity = SerialPort.NO_PARITY; break;
            case 1: parity = SerialPort.ODD_PARITY; break;
            case 2: parity = SerialPort.EVEN_PARITY; break;
            default: parity = SerialPort.NO_PARITY; break;
        }
        driver.setParity(parity);

        int flowControl;
        switch (props.getFlowControl()) {
            case 0: flowControl = SerialPort.FLOW_CONTROL_DISABLED; break;
            case 1: flowControl = SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED; break;
            case 2: flowControl = SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED | SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED; break;
            default: flowControl = SerialPort.FLOW_CONTROL_DISABLED; break;
        }
        driver.setFlowControl(flowControl);

        driver.setProtocol(props.getAstmProtocol());
        driver.setCharset(props.getCharsetName());

        return driver;
    }

    /**
     * FIX: Build a callback that translates state-machine status into Mirth
     * ConnectionStatusEvents AND calls channel.stop() on EXITING. This is the
     * single most important fix for the "channel silently stays STARTED"
     * bug — without this callback, the state machine cannot tell Mirth
     * anything has happened.
     */
    private AstmStatusCallback buildCallback() {
        return new AstmStatusCallback() {
            @Override
            public void reportStatus(AstmConnectionStatus status) {
                try {
                    switch (status) {
                        case STARTING:
                            logger.info("ASTM driver starting (channel="
                                    + safeChannelName() + ")");
                            setConnectorEvent(ConnectionStatusEventType.INFO, "Starting ASTM driver");
                            break;
                        case CONNECTING:
                            if (properties.getTransportMode() == AstmProperties.TransportMode.TCP_SERVER) {
                                String msg = "Listening for ASTM connections on port " + properties.getPort();
                                logger.info(msg + " (channel=" + safeChannelName() + ")");
                                setConnectorEvent(ConnectionStatusEventType.IDLE, msg);
                            } else if (properties.getTransportMode() == AstmProperties.TransportMode.SERIAL) {
                                String msg = "Opening serial port " + properties.getSerialPort()
                                        + " @ " + properties.getBaudRate() + " baud";
                                logger.info(msg + " (channel=" + safeChannelName() + ")");
                                setConnectorEvent(ConnectionStatusEventType.CONNECTING, msg);
                            } else {
                                String msg = "Connecting to " + properties.getHost() + ":" + properties.getPort();
                                logger.info(msg + " (channel=" + safeChannelName() + ")");
                                setConnectorEvent(ConnectionStatusEventType.CONNECTING, msg);
                            }
                            break;
                        case IDLE:
                            if (connector instanceof AstmReceiver) {
                                setConnectorEvent(ConnectionStatusEventType.CONNECTED,
                                        "Connected — waiting for incoming ASTM messages");
                            } else {
                                setConnectorEvent(ConnectionStatusEventType.CONNECTED,
                                        "Connected — waiting for outgoing ASTM messages");
                            }
                            break;
                        case RECEIVING:
                            if (connector instanceof AstmReceiver) {
                                setConnectorEvent(ConnectionStatusEventType.RECEIVING, "Receiving ASTM message");
                            } else {
                                setConnectorEvent(ConnectionStatusEventType.IDLE, "Stopping message sending");
                            }
                            break;
                        case SENDING:
                            if (connector instanceof AstmDispatcher) {
                                setConnectorEvent(ConnectionStatusEventType.SENDING, "Sending ASTM message");
                            } else {
                                setConnectorEvent(ConnectionStatusEventType.IDLE, "Stopping message receiving");
                            }
                            break;
                        case RECONNECTING:
                            logger.warn("ASTM connection lost, attempting reconnect (channel="
                                    + safeChannelName() + ")");
                            setConnectorEvent(ConnectionStatusEventType.DISCONNECTED, "Trying to reconnect");
                            break;
                        case DISCONNECTING:
                            setConnectorEvent(ConnectionStatusEventType.DISCONNECTED, "Disconnecting");
                            break;
                        case ERROR:
                            logger.error("ASTM driver reported ERROR status (channel="
                                    + safeChannelName() + ")");
                            setConnectorEvent(ConnectionStatusEventType.FAILURE, "ASTM driver error");
                            break;
                        case EXITING:
                            logger.warn("ASTM driver exiting — stopping channel "
                                    + safeChannelName() + " so Mirth dashboard reflects reality.");
                            setConnectorEvent(ConnectionStatusEventType.DISCONNECTED,
                                    "ASTM driver exited — channel will be stopped.");
                            // FIX: This is the critical bit. Without calling
                            // channel.stop(), Mirth leaves the channel in STARTED
                            // state forever, which is exactly the "silently enabled,
                            // no active" symptom reported.
                            if (connector != null && connector.getChannel() != null
                                    && connector.getCurrentState() != DeployedState.STOPPING
                                    && connector.getCurrentState() != DeployedState.STOPPED) {
                                try {
                                    connector.getChannel().stop();
                                } catch (StopException se) {
                                    logger.error("Failed to stop channel "
                                            + safeChannelName() + " after ASTM driver exit", se);
                                } catch (Throwable t) {
                                    logger.error("Unexpected error stopping channel "
                                            + safeChannelName() + " after ASTM driver exit", t);
                                }
                            }
                            break;
                    }
                } catch (Throwable t) {
                    // Never let the callback throw back into the state machine.
                    logger.error("Status callback threw while reporting " + status, t);
                }
            }
        };
    }

    private String safeChannelName() {
        try {
            return (connector != null && connector.getChannel() != null)
                    ? connector.getChannel().getName() : "<unknown>";
        } catch (Throwable t) {
            return "<unknown>";
        }
    }

    private String safeConnectorName() {
        if (connector instanceof SourceConnector) {
            try { return ((SourceConnector) connector).getSourceName(); } catch (Throwable t) {}
        } else if (connector instanceof DestinationConnector) {
            try { return ((DestinationConnector) connector).getDestinationName(); } catch (Throwable t) {}
        }
        return "ASTM";
    }

    private void setConnectorEvent(ConnectionStatusEventType event, String info) {
        if (eventController == null || connector == null) return;
        try {
            eventController.dispatchEvent(new ConnectionStatusEvent(
                    connector.getChannelId(),
                    connector.getMetaDataId(),
                    safeConnectorName(),
                    event,
                    info));
        } catch (Throwable t) {
            logger.warn("Failed to dispatch ConnectionStatusEvent: " + t.getMessage());
        }
    }

    // Called by AstmReceiver / AstmDispatcher after init()
    public void startDriver() throws Exception {
        if (driver == null) throw new IllegalStateException(
                "Driver not initialized. Call init() first.");
        logger.info("Starting ASTM driver (mode=" + properties.getTransportMode()
                + ", channel=" + safeChannelName() + ")");
        driver.start();
    }

    public void stopDriver() throws Exception {
        if (driver != null) {
            logger.info("Stopping ASTM driver (channel=" + safeChannelName() + ")");
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

    // Legacy ServerPlugin-style lifecycle — kept for backward compatibility.
    // Mirth 4.x plugins can implement ServerPlugin, but our AstmReceiver/
    // AstmDispatcher now own the lifecycle via onStart()/onStop().
    @Override
    public void start() {
        logger.info("AstmService plugin started");
    }

    @Override
    public void stop() {
        try {
            if (driver != null) driver.stop();
        } catch (Exception e) {
            logger.error("Error stopping ASTM service", e);
        }
    }

    // REQUIRED by ServerPlugin interface
    @Override
    public String getPluginPointName() {
        return "ASTM Settings";
    }
}
