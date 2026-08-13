package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.states.bundle.AstmContext;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmStatusCallback;
import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;

/**
 * Executes state loop, reports status to callbacks.
 *
 * IMPORTANT: This class is the ONLY bridge between the low-level ASTM protocol
 * state machine and Mirth Connect. When the state machine exits (normally or
 * abnormally), it MUST notify all registered callbacks so Mirth can stop the
 * channel. Otherwise the channel appears "silently started" while nothing is
 * actually listening.
 */
public class AstmStateMachine implements Closeable {
    private static final Logger logger = Logger.getLogger(AstmStateMachine.class.getName());

    private Thread stateThread;
    AstmState currentState;
    AstmContext context;
    Set<AstmStatusCallback> callbacks;
    private int accessCount = 0;

    // FIX: Track liveness so AstmReceiverService can detect a dead driver
    // instead of blocking forever on incomingQueue.take().
    private final AtomicBoolean alive = new AtomicBoolean(false);

    private Runnable stateLoop = new Runnable() {
        public final void run() {
            AstmStateMachine.this.alive.set(true);
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

                logger.info("ASTM State machine exiting normally");
                iter = AstmStateMachine.this.callbacks.iterator();
                while (iter.hasNext()) {
                    iter.next().reportStatus(AstmConnectionStatus.EXITING);
                }
                AstmStateMachine.this.currentState.run();
            } catch (Throwable t) {
                // FIX: log at ERROR (not FATAL). FATAL is hidden in Mirth's default
                // log4j config, which is why users see "no log error silently".
                logger.error("Unexpected exception in ASTM state machine, aborting execution", t);
                Iterator<AstmStatusCallback> iter = AstmStateMachine.this.callbacks.iterator();
                while (iter.hasNext()) {
                    try {
                        iter.next().reportStatus(AstmConnectionStatus.ERROR);
                    } catch (Throwable cbError) {
                        logger.error("Status callback threw while reporting ERROR", cbError);
                    }
                }
                // Always report EXITING so Mirth stops the channel
                iter = AstmStateMachine.this.callbacks.iterator();
                while (iter.hasNext()) {
                    try {
                        iter.next().reportStatus(AstmConnectionStatus.EXITING);
                    } catch (Throwable cbError) {
                        logger.error("Status callback threw while reporting EXITING", cbError);
                    }
                }
            } finally {
                AstmStateMachine.this.alive.set(false);
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

    /**
     * FIX: Returns true if the state-machine thread is still running.
     * AstmReceiverService polls this to detect a dead driver instead of
     * blocking forever on incomingQueue.take().
     */
    public final boolean isAlive() {
        return alive.get() && this.stateThread != null && this.stateThread.isAlive();
    }

    public final void start() throws IOException {
        this.stateThread = new Thread(this.stateLoop);
        this.stateThread.setDaemon(true);
        this.stateThread.setName("AstmStateMachine-" + System.identityHashCode(this));
        this.stateThread.start();
    }

    @Override
    public void close() throws IOException {
        logger.debug("Interrupting state machine thread");
        if (this.stateThread != null) {
            this.stateThread.interrupt();
        }
        try {
            if (this.currentState != null) {
                this.currentState.close();
            }
        } catch (NullPointerException e) {
        }
        if (this.stateThread == null) {
            return;
        }
        try {
            this.stateThread.join(10000L);
            if (this.stateThread.isAlive()) {
                logger.error("Thread still alive, retrying to close");
                this.context.getConnection().close();
                int retries = 0;
                while (this.stateThread.isAlive()) {
                    logger.debug("Stopping ASTM connection");
                    this.stateThread.interrupt();
                    this.stateThread.join(1000L);
                    ++retries;
                    if (retries % 10 == 0) {
                        logger.debug("Thread not stopped after " + retries + " retries");
                        this.context.getConnection().close();
                    }
                }
            }
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }
}
