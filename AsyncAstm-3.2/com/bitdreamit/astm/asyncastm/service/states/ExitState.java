package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.states.bundle.AstmContext;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;

/**
 * Terminal state. State machine exits after reaching this.
 */
public class ExitState extends AstmState {

    public ExitState(AstmContext context) {
        super(context);
    }

    @Override
    public final String getName() {
        return "Exit";
    }

    @Override
    public final AstmConnectionStatus getStatus() {
        return AstmConnectionStatus.EXITING;
    }

    @Override
    protected void execute() {
        // Terminal state — do nothing
    }
}
