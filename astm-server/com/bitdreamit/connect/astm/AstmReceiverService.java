package com.bitdreamit.connect.astm;

import com.bitdreamit.astm.asyncastm.AsyncAstmDriver;
import com.bitdreamit.astm.asyncastm.service.states.bundle.ReceivedMessage;
import com.bitdreamit.astm.asyncastm.service.states.bundle.TransmissionResult.Status;
import com.mirth.connect.donkey.model.event.ErrorEventType;
import com.mirth.connect.donkey.model.message.RawMessage;
import com.mirth.connect.donkey.server.channel.DispatchResult;
import com.mirth.connect.donkey.server.event.ErrorEvent;
import com.mirth.connect.server.controllers.ControllerFactory;
import com.mirth.connect.server.controllers.EventController;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AstmReceiverService implements Runnable {
    private static final Logger logger = Logger.getLogger(AstmReceiverService.class);

    private final AstmReceiver source;
    private final AsyncAstmDriver asyncAstm;
    private final EventController eventController = ControllerFactory.getFactory().createEventController();
    // FIX: Shared atomic flag instead of getCurrentState() race condition
    private final AtomicBoolean stopped;

    public AstmReceiverService(AstmReceiver source, AsyncAstmDriver asyncAstm, AtomicBoolean stopped) {
        this.source = source;
        this.asyncAstm = asyncAstm;
        this.stopped = stopped;
    }

    public void run() {
        String channelName = safeChannelName();
        logger.info("ASTM listener thread started for channel: " + channelName);
        try {
            while (!stopped.get()) {
                // FIX: Before blocking on getReceivedMessage(), check that the
                // underlying driver's state machine is still alive. If the driver
                // died (port in use, serial port not found, etc.), the state
                // machine thread has exited — we should NOT block forever on
                // incomingQueue.take(). Instead, dispatch a fatal error event to
                // Mirth and break out of the loop so Mirth sees the channel go
                // through real STOPPED state via the callback's channel.stop()
                // call (which AstmService.buildCallback() triggers on EXITING).
                if (!asyncAstm.isAlive()) {
                    String msg = "ASTM driver is no longer alive for channel "
                            + channelName + ". This usually means the connection "
                            + "could not be established (port in use, serial port "
                            + "not found, protocol mismatch, etc.). Listener thread exiting. "
                            + "Check earlier log entries for the root cause.";
                    logger.error(msg);
                    eventController.dispatchEvent(new ErrorEvent(
                            source.getChannelId(), source.getMetaDataId(),
                            (Long) null, ErrorEventType.SOURCE_CONNECTOR,
                            source.getSourceName(),
                            source.getConnectorProperties().getName(),
                            "ASTM driver died — channel listener stopping", new RuntimeException(msg)));
                    break;
                }

                try {
                    Map<String, Object> sourceMap = new HashMap<>();
                    // FIX: poll with a 5-second timeout so we can periodically
                    // re-check `stopped` and `asyncAstm.isAlive()`. Previously
                    // this called `getReceivedMessage()` which blocks forever
                    // on `incomingQueue.take()` — so if the driver died, the
                    // listener thread blocked forever and the channel appeared
                    // "silently started but not active".
                    ReceivedMessage received = asyncAstm.pollReceivedMessage(5, TimeUnit.SECONDS);

                    // Null = poll timeout, no message arrived. Loop back and
                    // re-check stopped / isAlive() at the top.
                    if (received == null) {
                        continue;
                    }

                    if (received.getResult().getStatus() == Status.SUCCESS) {
                        DispatchResult dispatchResult = null;
                        try {
                            dispatchResult = source.dispatchRawMessage(
                                    new RawMessage(received.getMessage(), (Collection) null, sourceMap));
                        } catch (Exception dispatchEx) {
                            // Per-message failure: log, but keep listening
                            logger.error("Failed to dispatch ASTM message to Mirth channel. Message dropped.", dispatchEx);
                        } finally {
                            if (dispatchResult != null) {
                                try {
                                    source.finishDispatch(dispatchResult);
                                } catch (Exception e) {
                                    logger.error("Error finishing dispatch", e);
                                }
                            }
                        }
                    } else {
                        Exception exception = new Exception(
                                received.getResult().getStatus().name() + ": " + received.getResult().getDescription());
                        StringBuilder receivedStr = new StringBuilder();
                        receivedStr.append("\nRecovered message:\n");
                        receivedStr.append(received.getMessage());
                        logger.error("ASTM Receiver exception (channel: "
                                + channelName + ")" + receivedStr.toString(), exception);
                        eventController.dispatchEvent(new ErrorEvent(
                                source.getChannelId(), source.getMetaDataId(),
                                (Long) null, ErrorEventType.SOURCE_CONNECTOR,
                                source.getSourceName(),
                                source.getConnectorProperties().getName(),
                                "Error receiving ASTM message", exception));
                    }
                } catch (InterruptedException ie) {
                    logger.debug("Listener thread interrupted");
                    Thread.currentThread().interrupt();
                    break;
                } catch (RuntimeException ex) {
                    // Recoverable per-message error: log and continue
                    logger.error("Recoverable error processing ASTM message. Continuing to listen.", ex);
                    // Brief sleep to avoid tight error loop burning CPU
                    try { Thread.sleep(1000); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt(); break;
                    }
                }
            }
        } catch (Throwable fatal) {
            logger.error("ASTM Listener fatal exception. Channel "
                    + channelName + " listener has been disabled", fatal);
            try {
                eventController.dispatchEvent(new ErrorEvent(
                        source.getChannelId(), source.getMetaDataId(),
                        (Long) null, ErrorEventType.SOURCE_CONNECTOR,
                        source.getSourceName(),
                        source.getConnectorProperties().getName(),
                        "Fatal Error in ASTM Listener", fatal));
            } catch (Throwable t) {
                logger.error("Failed to dispatch fatal error event", t);
            }
        }
        logger.info("ASTM listener thread exited for channel: " + channelName);
    }

    private String safeChannelName() {
        try {
            return source.getChannel() != null ? source.getChannel().getName() : "<unknown>";
        } catch (Throwable t) {
            return "<unknown>";
        }
    }
}
