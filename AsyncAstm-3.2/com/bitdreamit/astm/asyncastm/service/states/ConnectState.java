package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.states.bundle.AstmContext;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import java.io.IOException;
import java.net.SocketException;
import org.apache.log4j.Logger;

/**
 * Initiates TCP/Serial connection.
 *
 * FIX (Bug #15 — USB-COM recovery):
 * When doConnect() succeeds, reset the ReconnectState attempt counter so
 * the next failure starts backoff from 5s (not from where it left off).
 * This means: after a successful recovery, the channel behaves as if it
 * was freshly started.
 */
public class ConnectState extends AstmState {
    private static final Logger logger = Logger.getLogger(ConnectState.class.getName());

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
            context.getConnection().doConnect();
            // FIX (Bug #15): Reset the reconnect attempt counter on success.
            // This lets the channel recover gracefully: if it was stuck in
            // reconnect for 60s (backoff reached max), the next failure starts
            // fresh at 5s.
            try {
                AstmState reconnectState = context.getState(ReconnectState.class);
                if (reconnectState instanceof ReconnectState) {
                    ((ReconnectState) reconnectState).resetAttempts();
                }
            } catch (Throwable t) {
                // Don't let a state-cache issue break the connect path.
                logger.debug("Could not reset reconnect attempt counter", t);
            }
            logger.info("Connection established — transitioning to Idle");
            transitionTo(IdleState.class);
        } catch (InterruptedException e) {
            transitionTo(DisconnectState.class);
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        context.getConnection().close();
    }
}
