package com.bitdreamit.connect.astm;

import com.mirth.connect.donkey.server.channel.DispatchResult;
import com.mirth.connect.donkey.server.channel.SourceConnector;
import org.apache.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

public class AstmReceiver extends SourceConnector {
    private static final Logger logger = Logger.getLogger(AstmReceiver.class);

    private AstmService astmService;
    private AstmProperties properties;
    // FIX: AtomicBoolean shared with background thread for reliable shutdown signal
    private final AtomicBoolean stopped = new AtomicBoolean(true);

    @Override
    public void onDeploy() {
        logger.info("AstmReceiver deployed");
    }

    @Override
    public void onUndeploy() {
        logger.info("AstmReceiver undeployed");
    }

    @Override
    public void onStart() {
        properties = (AstmProperties) getConnectorProperties();
        astmService = new AstmService();
        astmService.init(properties);

        try {
            // FIX: Start driver BEFORE setting stopped=false so thread sees ready state
            astmService.startDriver();
            stopped.set(false);
            logger.info("AstmReceiver started with mode: " + properties.getTransportMode());

            AstmReceiverService receiverService = new AstmReceiverService(this, astmService.getDriver(), stopped);
            Thread receiverThread = new Thread(receiverService);
            receiverThread.setName("AstmReceiver-" + getChannelId());
            receiverThread.setDaemon(true);
            receiverThread.start();

        } catch (Exception e) {
            stopped.set(true);
            logger.error("Failed to start ASTM receiver", e);
            throw new RuntimeException("ASTM receiver start failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void onStop() {
        stopped.set(true);
        try {
            if (astmService != null) {
                astmService.stopDriver();
            }
        } catch (Exception e) {
            logger.error("Error stopping ASTM receiver", e);
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
            logger.error("Error halting ASTM receiver", e);
        }
    }

    @Override
    public void handleRecoveredResponse(DispatchResult dispatchResult) {
        // No recovery handling needed for ASTM
    }
}