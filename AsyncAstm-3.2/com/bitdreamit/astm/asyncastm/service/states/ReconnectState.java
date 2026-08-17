package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.states.bundle.AstmContext;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import java.io.IOException;
import org.apache.log4j.Logger;

/**
 * ReconnectState — closes the connection and transitions back to ConnectState
 * to retry the connection.
 *
 * FIX (Bug #15 — USB-COM hang / unplug recovery):
 * Previously this state just closed the connection and immediately transitioned
 * back to ConnectState. If the USB-RS232 adapter was unplugged or hung, this
 * produced a tight CPU-burning loop with no logging:
 *
 *     ConnectState -> IOException -> ReconnectState -> ConnectState -> IOException -> ...
 *
 * Now: sleep with exponential backoff (5s -> 10s -> 20s -> 40s -> 60s cap)
 * before retrying. Log every attempt at WARN level so the operator can see
 * the recovery in progress. Each attempt also fires a RECONNECTING
 * ConnectionStatusEvent via the state machine's callback chain, so the
 * Mirth dashboard shows "Trying to reconnect (attempt N)".
 *
 * The attempt counter resets when ConnectState succeeds (see ConnectState).
 * This means: once the USB-COM is plugged back in and the next connect
 * succeeds, the channel returns to normal operation and the backoff is reset
 * to 5s for the next failure.
 *
 * For USB-COM specifically:
 *   - Unplug -> jSerialComm's InputStream.read() returns -1 (EOF)
 *   - Reader thread sets lastByte=-1, releases readySemaphore, exits
 *   - Next readByteImpl() throws EOFException
 *   - AstmState.run() catches EOFException -> transitionTo(ReconnectState.class)
 *   - ReconnectState sleeps 5s, closes, transitions to ConnectState
 *   - ConnectState tries openPort() — if still unplugged, throws IOException
 *   - Loop with 5s/10s/20s/40s/60s backoff until USB is plugged back in
 *   - When USB is back, openPort() succeeds -> IdleState -> ready to receive
 *
 * For USB-COM hang (driver bug — read() blocks forever):
 *   - The 5-second read timeout (setComPortTimeouts) should handle this
 *   - If it doesn't, the channel will hang — manual restart required
 *   - TODO: add a watchdog thread that periodically checks isOpen() and
 *     forces reconnect if the connection appears hung
 */
public class ReconnectState extends AstmState {
    private static final Logger logger = Logger.getLogger(ReconnectState.class.getName());

    /** Base delay between reconnect attempts (first attempt waits this long). */
    private static final long BASE_DELAY_MS = 5000L;

    /** Maximum delay between reconnect attempts (backoff cap). */
    private static final long MAX_DELAY_MS = 60000L;

    /** Per-instance attempt counter (state instances are cached in AstmContext). */
    private int attemptCount = 0;

    public ReconnectState(AstmContext context) {
        super(context);
    }

    @Override
    public final String getName() {
        return "Reconnect";
    }

    @Override
    public final AstmConnectionStatus getStatus() {
        return AstmConnectionStatus.RECONNECTING;
    }

    /**
     * Reset the attempt counter. Called by ConnectState when the connection
     * is successfully established, so the next failure starts backoff from 5s.
     */
    public void resetAttempts() {
        if (attemptCount != 0) {
            logger.info("Reconnect attempt counter reset (was " + attemptCount
                + ") — connection successfully established");
            attemptCount = 0;
        }
    }

    @Override
    public final void execute() throws IOException {
        attemptCount++;

        // Exponential backoff: 5s, 10s, 20s, 40s, 60s, 60s, 60s, ...
        // Cap the shift at 10 to avoid overflow (2^10 = 1024, * 5s = 5120s, but capped at 60s).
        long shift = Math.min(attemptCount - 1, 10);
        long delayMs = Math.min(MAX_DELAY_MS, BASE_DELAY_MS * (1L << shift));

        String connDesc;
        try {
            connDesc = context.getConnection().getAddress().toString();
        } catch (Throwable t) {
            connDesc = "(unknown)";
        }

        // Log at WARN so it's visible at the default Mirth log level (INFO).
        // Use ERROR every 5 attempts to escalate visibility for ops dashboards.
        if (attemptCount == 1) {
            logger.warn("Connection lost (" + connDesc + ") — will retry in "
                + (delayMs / 1000) + "s (attempt #" + attemptCount + ")");
        } else if (attemptCount % 5 == 0) {
            logger.error("Still unable to reconnect after " + attemptCount
                + " attempts (" + connDesc + ") — retrying in "
                + (delayMs / 1000) + "s. Check USB-COM cable / analyzer power.");
        } else {
            logger.warn("Reconnect attempt #" + attemptCount + " (" + connDesc
                + ") — waiting " + (delayMs / 1000) + "s before retrying");
        }

        // Sleep before retrying. This is what prevents the tight CPU-burning loop.
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            logger.info("Reconnect wait interrupted — transitioning to Disconnect");
            Thread.currentThread().interrupt();
            transitionTo(DisconnectState.class);
            return;
        }

        // Close the old connection (frees the serial port / socket FD).
        try {
            context.getConnection().close();
        } catch (IOException e) {
            logger.warn("Error closing connection during reconnect (continuing): "
                + e.getMessage());
            // Continue anyway — we want to try to re-open the port.
        } catch (Throwable t) {
            logger.warn("Unexpected error closing connection during reconnect", t);
        }

        // Transition back to ConnectState — it will try doConnect() again.
        transitionTo(ConnectState.class);
    }
}
