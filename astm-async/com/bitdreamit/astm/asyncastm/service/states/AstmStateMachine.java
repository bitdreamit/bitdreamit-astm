package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.states.bundle.AstmContext;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmStatusCallback;
import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;

/**
 * Executes state loop, reports status to callbacks.
 */
public class AstmStateMachine implements Closeable {
    private static final Logger logger = Logger.getLogger(AstmStateMachine.class.getName());

    private Thread stateThread;
    AstmState currentState;
    AstmContext context;
    Set<AstmStatusCallback> callbacks;
    private int accessCount = 0;

    private Runnable stateLoop = new Runnable() {
        public final void run() {
            AstmStateMachine.this.currentState = new InitialState(AstmStateMachine.this.context);
            try {
                Iterator<AstmStatusCallback> iter;
                do {
                    logger.debug("Executing state " + AstmStateMachine.this.currentState.getName());
                    if (AstmStateMachine.this.currentState.isInitialized()) {
                        iter = AstmStateMachine.this.callbacks.iterator();
                        while (iter.hasNext()) {
                            iter.next().reportStatus(AstmStateMachine.this.currentState.getStatus());
                        }
                    }
                    AstmStateMachine.this.currentState = AstmStateMachine.this.currentState.run();
                } while (!(AstmStateMachine.this.currentState instanceof ExitState));

                logger.debug("ASTM State machine exiting");
                iter = AstmStateMachine.this.callbacks.iterator();
                while (iter.hasNext()) {
                    iter.next().reportStatus(AstmConnectionStatus.EXITING);
                }
                AstmStateMachine.this.currentState.run();
            } catch (Exception e) {
                logger.fatal("Unexpected exception in state machine, aborting execution", e);
                Iterator<AstmStatusCallback> iter = AstmStateMachine.this.callbacks.iterator();
                while (iter.hasNext()) {
                    iter.next().reportStatus(AstmConnectionStatus.ERROR);
                }
            }
        }
    };

    public final synchronized void addAccess() {
        ++this.accessCount;
        logger.debug("StateMachine access added (" + this.accessCount + " access)");
    }

    public final synchronized void removeAccess() {
        if (this.accessCount != 0) {
            --this.accessCount;
            logger.debug("StateMachine access removed (" + this.accessCount + " access)");
        } else {
            logger.debug("There weren't any access already");
        }
    }

    public final synchronized int getAccessCount() {
        return this.accessCount;
    }

    public AstmStateMachine(AstmContext context) {
        this.context = context;
        this.callbacks = ConcurrentHashMap.newKeySet();
    }

    public final void addCallback(AstmStatusCallback callback) {
        this.callbacks.add(callback);
    }

    public final void removeCallback(AstmStatusCallback callback) {
        this.callbacks.remove(callback);
    }

    public final AstmContext getContext() {
        return this.context;
    }

    public final AstmConnectionStatus getCurrentStatus() {
        return this.currentState == null ? AstmConnectionStatus.STARTING : this.currentState.getStatus();
    }

    public final void start() throws IOException {
        this.stateThread = new Thread(this.stateLoop);
        this.stateThread.start();
    }

    @Override
    public void close() throws IOException {
        logger.debug("Interrupting state machine thread");
        this.stateThread.interrupt();
        try {
            this.currentState.close();
        } catch (NullPointerException e) {
        }
        try {
            this.stateThread.join(10000L);
            if (this.stateThread.isAlive()) {
                logger.error("Thread still alive, retrying to close");
                this.context.getConnection().close();
                int retries = 0;
                // BIDIRECTIONAL FIX (Bug #23): bound the retry loop. The old
                // while(isAlive()) NEVER gave up, so one stuck state thread
                // (native serial read) wedged Mirth's undeploy/deploy queue
                // for every channel. Give the thread 10 more seconds, then
                // log and move on — the thread is a daemon and dies when the
                // connection object is collected / port is closed.
                while (this.stateThread.isAlive() && retries < 10) {
                    logger.debug("Stopping ASTM connection");
                    this.stateThread.interrupt();
                    this.stateThread.join(1000L);
                    ++retries;
                    if (retries % 10 == 0) {
                        logger.debug("Thread not stopped after " + retries + " retries");
                        this.context.getConnection().close();
                    }
                }
                if (this.stateThread.isAlive()) {
                    logger.error("ASTM state thread did not stop within the grace period — "
                        + "giving up so the channel lifecycle can proceed (daemon thread)");
                }
            }
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }
}
