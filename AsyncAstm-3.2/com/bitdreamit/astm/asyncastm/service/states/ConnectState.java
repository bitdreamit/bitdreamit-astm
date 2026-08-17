package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.states.bundle.AstmContext;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import java.io.IOException;
import java.net.SocketException;
import org.apache.log4j.Logger;

/**
 * Initiates TCP/Serial connection.
 *
 * SIMPLE SOLUTION (Bug #15):
 * On successful connect, reset the ReconnectState attempt counter so the
 * next failure starts backoff from 5s.
 */
public class ConnectState extends AstmState {
    private static final Logger logger = Logger.getLogger(ConnectState.class.getName());

    public ConnectState(AstmContext context) {
        super(context);
    }

    @Override
    public final String getName() { return "Connect"; }

    @Override
    public final AstmConnectionStatus getStatus() { return AstmConnectionStatus.CONNECTING; }

    @Override
    protected final void init() throws SocketException, IOException {
        super.init();
    }

    @Override
    public final void execute() throws IOException, InterruptedException {
        try {
            context.getConnection().doConnect();
            // Reset the reconnect attempt counter on success.
            try {
                AstmState reconnectState = context.getState(ReconnectState.class);
                if (reconnectState instanceof ReconnectState) {
                    ((ReconnectState) reconnectState).resetAttempts();
                }
            } catch (Throwable t) {
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
