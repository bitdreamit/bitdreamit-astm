package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.states.bundle.AstmContext;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import java.io.IOException;

/**
 * Closes socket, transitions back to Connect.
 */
public class ReconnectState extends AstmState {

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

    @Override
    public final void execute() throws IOException {
        try {
            context.getConnection().close();
            transitionTo(ConnectState.class);
        } catch (IOException e) {
            transitionTo(DisconnectState.class);
        }
    }
}
