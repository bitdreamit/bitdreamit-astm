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
 *
 * FIX (Bug #9): run() previously only caught InterruptedException and EOFException.
 * Any RuntimeException thrown by execute() (e.g., from AstmSerialConnection.doConnect()
 * when the serial port fails to open, or from a NullPointerException in the reader
 * thread bridge) propagated up to AstmStateMachine.stateLoop's catch(Exception),
 * which logged at FATAL and killed the state machine entirely. The channel would
 * appear "Started" in Mirth but be completely dead.
 *
 * Now: RuntimeException is caught at the state level, logged at ERROR, and
 * triggers a transition to ReconnectState so the state machine can retry
 * (or, if ReconnectState also fails, to DisconnectState → ExitState).
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
            logger.debug("ASTM disconnection (EOF)");
            transitionTo(ReconnectState.class);
        } catch (IOException e) {
            // FIX (Bug #9): Treat IOException as a transient failure — try to reconnect.
            logger.warn("I/O error in state " + getName() + ", will attempt to reconnect: " + e.getMessage());
            transitionTo(ReconnectState.class);
        } catch (RuntimeException e) {
            // FIX (Bug #9): Previously RuntimeException propagated all the way up
            // and killed the state machine silently (well, FATAL log, but no Mirth
            // event). Now we catch it here, log it loudly, and try to reconnect.
            // If reconnect also fails, DisconnectState → ExitState will cleanly
            // shut down the channel.
            logger.error("Unexpected runtime exception in state " + getName()
                + ", attempting reconnect", e);
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
