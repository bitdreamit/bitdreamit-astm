package com.bitdreamit.connect.astm;

import com.mirth.connect.donkey.model.channel.DeployedState;
import com.mirth.connect.donkey.model.event.ConnectionStatusEventType;
import com.mirth.connect.donkey.server.channel.DispatchResult;
import com.mirth.connect.donkey.server.channel.SourceConnector;
import com.mirth.connect.server.controllers.ControllerFactory;
import com.mirth.connect.server.controllers.EventController;
import com.mirth.connect.donkey.server.event.ConnectionStatusEvent;
import com.bitdreamit.astm.asyncastm.AsyncAstmDriver;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmStatusCallback;
import org.apache.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AstmReceiver — Mirth Source Connector that listens for incoming ASTM messages
 * over TCP (client or server mode) or Serial (RS-232).
 *
 * FIX (Bug #4): The v3.0.2 version never registered an AstmStatusCallback with
 * the driver, so the Mirth dashboard connector status badge stayed blank and no
 * ConnectionStatusEvent was ever dispatched — the "channel not enable" symptom.
 * Now we construct a proper callback that translates ASTM state machine status
 * changes into Mirth ConnectionStatusEvent dispatches.
 */
public class AstmReceiver extends SourceConnector {
    private static final Logger logger = Logger.getLogger(AstmReceiver.class);

    private AstmService astmService;
    private AstmProperties properties;
    private final AtomicBoolean stopped = new AtomicBoolean(true);

    @Override
    public void onDeploy() {
        logger.info("AstmReceiver deployed for channel " + getChannelId());
    }

    @Override
    public void onUndeploy() {
        logger.info("AstmReceiver undeployed for channel " + getChannelId());
    }

    @Override
    public void onStart() {
        properties = (AstmProperties) getConnectorProperties();
        astmService = new AstmService();

        // FIX (Bug #4): Build the status callback BEFORE init() so the driver
        // is constructed with the callback already attached. The callback
        // dispatches Mirth ConnectionStatusEvents so the dashboard reflects
        // the actual state of the ASTM driver.
        AstmStatusCallback statusCallback = buildStatusCallback(properties);

        try {
            astmService.init(properties, statusCallback);
            // Start driver BEFORE setting stopped=false so the listener thread
            // sees the ready state. If startDriver() throws (e.g., serial port
            // cannot be opened), we mark stopped=true and rethrow so Mirth
            // marks the channel as failed.
            astmService.startDriver();
            stopped.set(false);
            logger.info("AstmReceiver started: mode=" + properties.getTransportMode()
                + ", channel=" + getChannelId());

            AstmReceiverService receiverService = new AstmReceiverService(
                this, astmService.getDriver(), stopped);
            Thread receiverThread = new Thread(receiverService);
            receiverThread.setName("AstmReceiver-" + getChannelId());
            receiverThread.setDaemon(true);
            receiverThread.start();

        } catch (Exception e) {
            stopped.set(true);
            logger.error("Failed to start ASTM receiver for channel " + getChannelId(), e);
            throw new RuntimeException("ASTM receiver start failed: " + e.getMessage(), e);
        }
    }

    /**
     * Build an AstmStatusCallback that translates ASTM driver state changes
     * into Mirth ConnectionStatusEvent dispatches. This is what makes the
     * Mirth dashboard connector status badge actually update.
     *
     * Ported from the deprecated AstmConnectionManager logic, adapted for
     * the new transportMode-based properties.
     */
    private AstmStatusCallback buildStatusCallback(final AstmProperties props) {
        final EventController ec = ControllerFactory.getFactory().createEventController();
        final String channelId = getChannelId();
        final int metaDataId = getMetaDataId();
        final String connectorName = getSourceName();

        return new AstmStatusCallback() {
            @Override
            public void reportStatus(AstmConnectionStatus status) {
                try {
                    ConnectionStatusEventType type;
                    String info;
                    switch (status) {
                        case CONNECTING:
                            if (props.getTransportMode() == AstmProperties.TransportMode.SERIAL) {
                                type = ConnectionStatusEventType.CONNECTING;
                                info = "Opening serial port " + props.getSerialPort()
                                    + " @ " + props.getBaudRate();
                            } else if (props.getTransportMode() == AstmProperties.TransportMode.TCP_SERVER) {
                                type = ConnectionStatusEventType.IDLE;
                                info = "Listening on " + props.getHost() + ":" + props.getPort();
                            } else {
                                type = ConnectionStatusEventType.CONNECTING;
                                info = "Connecting to " + props.getHost() + ":" + props.getPort();
                            }
                            break;
                        case IDLE:
                            type = ConnectionStatusEventType.CONNECTED;
                            info = "Waiting for receiving messages";
                            break;
                        case RECEIVING:
                            type = ConnectionStatusEventType.RECEIVING;
                            info = "Receiving new message";
                            break;
                        case SENDING:
                            // Source connector doesn't initiate sends, but report anyway.
                            type = ConnectionStatusEventType.IDLE;
                            info = "Stopping message receiving";
                            break;
                        case RECONNECTING:
                            type = ConnectionStatusEventType.DISCONNECTED;
                            info = "Trying to reconnect";
                            break;
                        case DISCONNECTING:
                            type = ConnectionStatusEventType.IDLE;
                            info = "Disconnecting";
                            break;
                        case EXITING:
                            type = ConnectionStatusEventType.IDLE;
                            info = "Disconnected";
                            // If the state machine exits on its own (not because we stopped it),
                            // stop the channel so Mirth marks it as stopped rather than hanging.
                            if (getCurrentState() != DeployedState.STOPPING
                                && getCurrentState() != DeployedState.STOPPED) {
                                try {
                                    getChannel().stop();
                                } catch (Exception e) {
                                    logger.warn("Failed to auto-stop channel " + channelId
                                        + " after ASTM state machine exited", e);
                                }
                            }
                            break;
                        case ERROR:
                            type = ConnectionStatusEventType.FAILURE;
                            info = "ASTM driver error";
                            break;
                        case STARTING:
                        default:
                            type = ConnectionStatusEventType.INFO;
                            info = "Starting ASTM driver";
                    }
                    ec.dispatchEvent(new ConnectionStatusEvent(
                        channelId, metaDataId, connectorName, type, info));
                } catch (Exception e) {
                    logger.warn("Failed to dispatch ASTM connection status event", e);
                }
            }
        };
    }

    @Override
    public void onStop() {
        stopped.set(true);
        try {
            if (astmService != null) {
                astmService.stopDriver();
            }
        } catch (Exception e) {
            logger.error("Error stopping ASTM receiver for channel " + getChannelId(), e);
        }
    }

    @Override
    public void onHalt() {
        stopped.set(true);
        try {
            if (astmService != null) {
                astmService.stopDriver();
            }
        } catch (Exception e) {
            logger.error("Error halting ASTM receiver for channel " + getChannelId(), e);
        }
    }

    @Override
    public void handleRecoveredResponse(DispatchResult dispatchResult) {
        // No recovery handling needed for ASTM
    }
}
