package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.states.bundle.AstmContext;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import java.io.IOException;
import org.apache.log4j.Logger;

/**
 * ReconnectState — closes the connection, waits a bit, then tries to connect
 * again via ConnectState.
 *
 * SIMPLE SOLUTION (Bug #15):
 * When USB-COM is unplugged or analyzer drops, this state waits 5 seconds
 * (then 10s, 20s, 40s, 60s with exponential backoff) and retries. Logs at
 * WARN so the lab can see what's happening. Once the cable is back or the
 * analyzer reconnects, the channel returns to normal — NO manual restart.
 *
 * The attempt counter resets on successful connect (see ConnectState), so
 * the next failure starts backoff fresh from 5s.
 */
public class ReconnectState extends AstmState {
    private static final Logger logger = Logger.getLogger(ReconnectState.class.getName());

    private static final long BASE_DELAY_MS = 5000L;
    private static final long MAX_DELAY_MS = 60000L;

    private int attemptCount = 0;

    public ReconnectState(AstmContext context) {
        super(context);
    }

    @Override
    public final String getName() { return "Reconnect"; }

    @Override
    public final AstmConnectionStatus getStatus() { return AstmConnectionStatus.RECONNECTING; }

    /**
     * Reset the attempt counter. Called by ConnectState when the connection
     * is successfully established, so the next failure starts backoff from 5s.
     */
    public void resetAttempts() {
        if (attemptCount != 0) {
            logger.info("Reconnect attempt counter reset — connection established");
            attemptCount = 0;
        }
    }

    @Override
    public final void execute() throws IOException {
        attemptCount++;

        // Exponential backoff: 5s, 10s, 20s, 40s, 60s, 60s, 60s, ...
        long shift = Math.min(attemptCount - 1, 10);
        long delayMs = Math.min(MAX_DELAY_MS, BASE_DELAY_MS * (1L << shift));

        // Log at WARN so it's visible at the default Mirth log level (INFO).
        // Use ERROR every 5 attempts so it shows in the Mirth Messages view too.
        if (attemptCount == 1) {
            logger.warn("Connection lost — will retry in "
                + (delayMs / 1000) + "s (attempt #1)");
        } else if (attemptCount % 5 == 0) {
            logger.error("Still unable to reconnect after " + attemptCount
                + " attempts — retrying in " + (delayMs / 1000)
                + "s. Check USB cable / analyzer power.");
        } else {
            logger.warn("Reconnect attempt #" + attemptCount
                + " — waiting " + (delayMs / 1000) + "s");
        }

        // Sleep before retrying. This prevents the tight CPU-burning loop.
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            logger.info("Reconnect wait interrupted — stopping");
            Thread.currentThread().interrupt();
            transitionTo(DisconnectState.class);
            return;
        }

        // Close the old connection (frees the serial port / socket).
        try {
            context.getConnection().close();
        } catch (IOException e) {
            logger.warn("Error closing connection (continuing): " + e.getMessage());
        } catch (Throwable t) {
            logger.warn("Unexpected error closing connection (continuing)", t);
        }

        // Transition back to ConnectState — it will try doConnect() again,
        // which reads the latest channel properties (so COM port name
        // changes take effect here automatically).
        transitionTo(ConnectState.class);
    }
}
