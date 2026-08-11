package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.states.bundle.AstmContext;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import org.apache.log4j.Logger;

/**
 * Abstract base for all ASTM protocol states.
 */
public abstract class AstmState implements Closeable {
    private static final Logger logger = Logger.getLogger(AstmState.class.getName());

    private AstmState nextState;
    protected AstmContext context;
    private boolean initialized;

    public abstract String getName();
    public abstract AstmConnectionStatus getStatus();

    public AstmState(AstmContext context) {
        this.context = context;
        this.initialized = true;
    }

    protected abstract void execute() throws IOException, InterruptedException;

    protected void init() throws SocketException, IOException {
        Thread.currentThread().setName(getName() + " @ " + context.getConnection().getAddress());
        this.nextState = this;
    }

    public final AstmState run() throws IOException {
        if (this.initialized) {
            init();
            this.initialized = false;
        }
        try {
            if (Thread.interrupted() && !(this.nextState instanceof DisconnectState)) {
                throw new InterruptedException();
            }
            execute();
        } catch (InterruptedException e) {
            logger.debug("Interrupted state, setting next state to Disconnect");
            transitionTo(DisconnectState.class);
        } catch (EOFException e) {
            logger.debug("ASTM disconnection");
            transitionTo(ReconnectState.class);
        }
        return this.nextState;
    }

    public final synchronized void transitionTo(Class<? extends AstmState> stateClass) {
        this.nextState = context.getState(stateClass);
        this.nextState.initialized = true;
    }

    public final synchronized AstmState getNextState() {
        return this.nextState;
    }

    final boolean isInitialized() {
        return this.initialized;
    }

    @Override
    public void close() throws IOException {
    }
}
