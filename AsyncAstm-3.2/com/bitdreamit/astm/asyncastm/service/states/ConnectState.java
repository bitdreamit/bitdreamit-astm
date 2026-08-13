package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.states.bundle.AstmContext;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import java.io.IOException;
import java.net.SocketException;

/**
 * Initiates TCP/Serial connection.
 */
public class ConnectState extends AstmState {

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
