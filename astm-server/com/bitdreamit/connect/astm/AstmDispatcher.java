package com.bitdreamit.connect.astm;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.channel.DeployedState;
import com.mirth.connect.donkey.model.event.ConnectionStatusEventType;
import com.mirth.connect.donkey.model.message.ConnectorMessage;
import com.mirth.connect.donkey.model.message.MessageContent;
import com.mirth.connect.donkey.model.message.Response;
import com.mirth.connect.donkey.model.message.Status;
import com.mirth.connect.donkey.server.channel.DestinationConnector;
import com.mirth.connect.server.controllers.ControllerFactory;
import com.mirth.connect.server.controllers.EventController;
import com.mirth.connect.donkey.server.event.ConnectionStatusEvent;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmStatusCallback;
import org.apache.log4j.Logger;

import java.nio.charset.Charset;

/**
 * AstmDispatcher — Mirth Destination Connector that sends ASTM messages over
 * TCP (client or server mode) or Serial (RS-232).
 *
 * FIX (Bug #4): Register an AstmStatusCallback so Mirth dashboard reflects
 * actual driver state.
 *
 * FIX (Bug #10): send() now uses configured sendTimeout.
 *
 * FIX (Bug #15): ReconnectState has exponential backoff — auto-recovers from
 * connection drops without manual restart.
 */
public class AstmDispatcher extends DestinationConnector {
    private static final Logger logger = Logger.getLogger(AstmDispatcher.class);

    private AstmService astmService;
    private AstmProperties properties;

    @Override
    public void onDeploy() {
        logger.info("AstmDispatcher deployed for channel " + getChannelId());
    }

    @Override
    public void onUndeploy() {
        logger.info("AstmDispatcher undeployed for channel " + getChannelId());
    }

    @Override
    public void onStart() {
        try {
            properties = (AstmProperties) getConnectorProperties();
            astmService = new AstmService();
            AstmStatusCallback statusCallback = buildStatusCallback(properties);
            astmService.init(properties, statusCallback);

            // Start the driver in a background thread so onStart() returns
            // immediately. For TCP_CLIENT mode, startDriver() may block on
            // the initial connect attempt (with retry/backoff).
            final AstmService svc = astmService;
            Thread starter = new Thread(() -> {
                try {
                    svc.startDriver();
                    logger.info("AstmDispatcher driver started for channel " + getChannelId());
                } catch (Exception e) {
                    logger.error("AstmDispatcher driver start failed for channel "
                        + getChannelId(), e);
                }
            });
            starter.setName("AstmDispatcher-starter-" + getChannelId());
            starter.setDaemon(true);
            starter.start();
            logger.info("AstmDispatcher onStart() completed for channel " + getChannelId());
        } catch (Exception e) {
            logger.error("Failed to initialize ASTM dispatcher for channel " + getChannelId(), e);
            throw new RuntimeException("ASTM dispatcher init failed: " + e.getMessage(), e);
        }
    }

    private AstmStatusCallback buildStatusCallback(final AstmProperties props) {
        final EventController ec = ControllerFactory.getFactory().createEventController();
        final String channelId = getChannelId();
        final int metaDataId = getMetaDataId();
        final String connectorName = getDestinationName();

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
                                info = "Opening serial port " + props.getSerialPort();
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
                            info = "Waiting for sending messages";
                            break;
                        case SENDING:
                            type = ConnectionStatusEventType.SENDING;
                            info = "Sending new message";
                            break;
                        case RECEIVING:
                            type = ConnectionStatusEventType.IDLE;
                            info = "Stopping message sending";
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
        try {
            if (astmService != null) {
                astmService.stopDriver();
            }
        } catch (Exception e) {
            logger.error("Error stopping ASTM dispatcher for channel " + getChannelId(), e);
        }
    }

    @Override
    public void onHalt() {
        try {
            if (astmService != null) {
                astmService.stopDriver();
            }
        } catch (Exception e) {
            logger.error("Error halting ASTM dispatcher for channel " + getChannelId(), e);
        }
    }

    @Override
    public Response send(ConnectorProperties connectorProperties, ConnectorMessage message) {
        try {
            AstmProperties props = (AstmProperties) connectorProperties;
            MessageContent encoded = message.getEncoded();
            String payload = encoded != null ? encoded.getContent() : "";
            byte[] data = payload.getBytes(Charset.forName(props.getCharsetName()));

            // Wait for driver to be ready, honoring the configured sendTimeout.
            // Note: getSendTimeout() lives on AstmDispatcherProperties, not on the
            // AstmProperties base class. Cast to the concrete subtype to access it.
            long sendTimeoutMs = 20000;
            try {
                if (props instanceof AstmDispatcherProperties) {
                    String stStr = ((AstmDispatcherProperties) props).getSendTimeout();
                    if (stStr != null && !stStr.trim().isEmpty()) {
                        sendTimeoutMs = Long.parseLong(stStr.trim());
                    }
                }
            } catch (NumberFormatException ignored) {
                // keep default 20000 ms
            }
            long deadline = System.currentTimeMillis() + sendTimeoutMs;
            while (System.currentTimeMillis() < deadline) {
                if (astmService != null && astmService.getDriver() != null
                    && astmService.getDriver().isConnected()) {
                    break;
                }
                Thread.sleep(100);
            }
            if (astmService == null || astmService.getDriver() == null
                || !astmService.getDriver().isConnected()) {
                String errMsg = "ASTM driver not connected after " + sendTimeoutMs
                    + "ms — message not sent";
                logger.error(errMsg);
                return new Response(Status.ERROR, errMsg);
            }

            boolean sent = astmService.send(data);
            if (sent) {
                return new Response(Status.SENT, payload);
            } else {
                return new Response(Status.ERROR, "ASTM send returned false");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Response(Status.ERROR, "ASTM dispatch interrupted");
        } catch (Exception e) {
            logger.error("ASTM dispatch error", e);
            return new Response(Status.ERROR, e.getMessage());
        }
    }

    @Override
    public void replaceConnectorProperties(ConnectorProperties connectorProperties, ConnectorMessage message) {
        // No dynamic property replacement needed for ASTM
    }
}
