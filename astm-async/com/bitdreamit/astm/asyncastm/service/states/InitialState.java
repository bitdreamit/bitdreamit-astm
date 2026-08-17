package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.states.bundle.AstmContext;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;

import java.io.IOException;

/**
 * Entry point state. Immediately transitions to Connect.
 */
public class InitialState extends AstmState {

    public InitialState(AstmContext context) {
        super(context);
    }

    @Override
    public final String getName() {
        return "Initial";
    }

    @Override
    public final AstmConnectionStatus getStatus() {
        return AstmConnectionStatus.STARTING;
    }

    @Override
    protected void execute() throws IOException, InterruptedException {
        transitionTo(ConnectState.class);
    }
}
