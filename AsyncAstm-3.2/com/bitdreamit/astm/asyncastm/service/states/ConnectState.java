package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.states.bundle.AstmContext;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import java.io.IOException;
import java.net.SocketException;
import org.apache.log4j.Logger;

/**
 * Initiates TCP/Serial connection.
 *
 * FIX: If doConnect() fails (port already in use, serial port not found, etc.),
 * we now transition to ReconnectState instead of letting the exception escape
 * to AstmStateMachine's catch-all. This means:
 *   1. The state machine stays alive (no silent death).
 *   2. The status callback gets RECONNECTING (not ERROR/EXITING).
 *   3. The user sees a useful log line explaining what's happening.
 *
 * After N failed reconnect attempts (controlled by ReconnectState), we
 * eventually transition to DisconnectState → ExitState, which triggers
 * EXITING notification to Mirth so Mirth actually stops the channel.
 */
public class ConnectState extends AstmState {
    private static final Logger logger = Logger.getLogger(ConnectState.class.getName());

    private int connectAttempts = 0;
    private static final int MAX_CONNECT_ATTEMPTS = 10;
    private static final long RECONNECT_DELAY_MS = 5000;

    public ConnectState(AstmContext context) {
        super(context);
    }

    @Override
    public final String getName() {
        return "Connect";
    }

    @Override
    public final AstmConnectionStatus getStatus() {
        return AstmConnectionStatus.CONNECTING;
    }

    @Override
    protected final void init() throws SocketException, IOException {
        super.init();
    }

    @Override
    public final void execute() throws IOException, InterruptedException {
        try {
            logger.info("Attempting ASTM connection (attempt " + (connectAttempts + 1) + ")");
            context.getConnection().doConnect();
            logger.info("ASTM connection established");
            connectAttempts = 0; // reset on success
            transitionTo(IdleState.class);
        } catch (InterruptedException e) {
            logger.debug("Connect interrupted, transitioning to Disconnect");
            transitionTo(DisconnectState.class);
        } catch (IOException e) {
            // FIX: previously this escaped to AstmStateMachine.stateLoop and killed
            // the thread silently. Now we log it explicitly and retry.
            ++connectAttempts;
            logger.error("ASTM connection attempt " + connectAttempts + " failed: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage()
                    + (connectAttempts >= MAX_CONNECT_ATTEMPTS
                       ? " — giving up after " + MAX_CONNECT_ATTEMPTS + " attempts"
                       : " — will retry in " + (RECONNECT_DELAY_MS / 1000) + "s"));
            if (connectAttempts >= MAX_CONNECT_ATTEMPTS) {
                logger.error("Max connect attempts (" + MAX_CONNECT_ATTEMPTS
                        + ") reached. Transitioning to DisconnectState.");
                transitionTo(DisconnectState.class);
                return;
            }
            try {
                Thread.sleep(RECONNECT_DELAY_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                transitionTo(DisconnectState.class);
                return;
            }
            transitionTo(ReconnectState.class);
        } catch (RuntimeException e) {
            // Should not happen anymore (serial driver now throws IOException),
            // but guard against any leftover path.
            logger.error("ASTM connection failed with runtime exception: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
            ++connectAttempts;
            if (connectAttempts >= MAX_CONNECT_ATTEMPTS) {
                transitionTo(DisconnectState.class);
            } else {
                try { Thread.sleep(RECONNECT_DELAY_MS); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    transitionTo(DisconnectState.class);
                    return;
                }
                transitionTo(ReconnectState.class);
            }
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        context.getConnection().close();
    }
}
