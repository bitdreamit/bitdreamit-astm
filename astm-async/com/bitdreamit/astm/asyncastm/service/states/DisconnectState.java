package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.states.bundle.AstmContext;
import com.bitdreamit.astm.asyncastm.service.states.bundle.TransmissionResult;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import java.io.IOException;
import org.apache.log4j.Logger;

/**
 * Graceful shutdown. Cancels pending messages.
 */
public class DisconnectState extends AstmState {
    private static final Logger logger = Logger.getLogger(DisconnectState.class.getName());

    public DisconnectState(AstmContext context) {
        super(context);
    }

    @Override
    public final String getName() {
        return "Disconnect";
    }

    @Override
    public final AstmConnectionStatus getStatus() {
        return AstmConnectionStatus.DISCONNECTING;
    }

    @Override
    public final void execute() throws IOException {
        if (context.hasOutgoingMessage()) {
            String msg = "Outgoing message was deleted before being sent due to a disconnection";
            logger.error(msg);
            TransmissionResult result = new TransmissionResult(TransmissionResult.Status.DISCONNECTED, msg);
            context.setTransmissionResult(result);
        }
        context.getConnection().close();
        transitionTo(ExitState.class);
    }
}
