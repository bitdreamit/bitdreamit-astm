package com.bitdreamit.connect.astm;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.message.ConnectorMessage;
import com.mirth.connect.donkey.model.message.MessageContent;
import com.mirth.connect.donkey.model.message.Response;
import com.mirth.connect.donkey.model.message.Status;
import com.mirth.connect.donkey.server.channel.DestinationConnector;
import org.apache.log4j.Logger;

import java.nio.charset.Charset;

public class AstmDispatcher extends DestinationConnector {
    private static final Logger logger = Logger.getLogger(AstmDispatcher.class);

    private AstmService astmService;

    @Override
    public void onDeploy() {
        logger.info("AstmDispatcher deployed");
    }

    @Override
    public void onUndeploy() {
        logger.info("AstmDispatcher undeployed");
    }

    @Override
    public void onStart() {
        try {
            astmService = new AstmService();
            // FIX: pass `this` (the Connector) so AstmService can build a callback
            // that dispatches ConnectionStatusEvent and calls channel.stop() on
            // EXITING. This is the FIX for the "channel silently started" bug.
            astmService.init((AstmProperties) getConnectorProperties(), this);
            // FIX: For TCP client mode, startDriver() blocks on socket connect.
            // We start it in a background thread so onStart() returns immediately
            // and Mirth doesn't think the connector is hung.
            final AstmService svc = astmService;
            Thread starter = new Thread(() -> {
                try {
                    svc.startDriver();
                    logger.info("AstmDispatcher driver started in background");
                } catch (Exception e) {
                    logger.error("AstmDispatcher background driver start failed", e);
                }
            });
            starter.setName("AstmDispatcher-starter-" + getChannelId());
            starter.setDaemon(true);
            starter.start();
            logger.info("AstmDispatcher onStart() completed (driver starting in background)");
        } catch (Exception e) {
            logger.error("Failed to initialize ASTM dispatcher", e);
            throw new RuntimeException("ASTM dispatcher init failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void onStop() {
        try {
            if (astmService != null) {
                astmService.stopDriver();
            }
        } catch (Exception e) {
            logger.error("Error stopping ASTM dispatcher", e);
        }
    }

    @Override
    public void onHalt() {
        try {
            if (astmService != null) {
                astmService.stopDriver();
            }
        } catch (Exception e) {
            logger.error("Error halting ASTM dispatcher", e);
        }
    }

    @Override
    public Response send(ConnectorProperties connectorProperties, ConnectorMessage message) {
        try {
            AstmProperties props = (AstmProperties) connectorProperties;
            MessageContent encoded = message.getEncoded();
            String payload = encoded != null ? encoded.getContent() : "";
            byte[] data = payload.getBytes(Charset.forName(props.getCharsetName()));

            // Wait for driver to be ready if background start hasn't finished yet
            int retries = 50; // 5 seconds max
            while (retries-- > 0 && (astmService == null || !astmService.getDriver().isConnected())) {
                Thread.sleep(100);
            }

            boolean sent = astmService.send(data);
            if (sent) {
                return new Response(Status.SENT, payload);
            } else {
                return new Response(Status.ERROR, "ASTM send returned false");
            }
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