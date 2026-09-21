package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.connection.AstmControlChars;
import com.bitdreamit.astm.asyncastm.service.states.bundle.AstmContext;
import com.bitdreamit.astm.asyncastm.service.states.bundle.TransmissionResult;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import com.bitdreamit.astm.asyncastm.service.connection.file.IdleTimeoutTimer;
import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;

/**
 * Main state: waits for ENQ (incoming) or sends ENQ (outgoing).
 */
public class IdleState extends AstmState {
    private static final Logger logger = Logger.getLogger(IdleState.class.getName());

    private volatile boolean receivedEnq;
    private IdleTimeoutTimer timeoutTimer;
    private int retryCount;
    private Semaphore incomingSemaphore;
    private Semaphore outgoingSemaphore;

    private Runnable incomingWaiter = new Runnable() {
        public final void run() {
            IdleState.this.incomingSemaphore.release();
            try {
                IdleState.this.outgoingSemaphore.acquire();
                while (true) {
                    if (!IdleState.this.receivedEnq) {
                        logger.debug("Waiting for incoming messages");
                        try {
                            int b = IdleState.this.context.getConnection().readByteBlocking();
                            if (b == 5) { // ENQ
                                logger.debug("Received ENQ request");
                                IdleState.this.receivedEnq = true;
                            } else {
                                logger.warn("Error, ENQ expected. Received: " + AstmControlChars.name(b));
                            }
                            continue;
                        } catch (EOFException e) {
                            logger.warn("End of stream reached, connection was closed");
                            transitionTo(ReconnectState.class);
                        }
                    }
                    logger.debug("Stopped waiting for incoming messages");
                    return;
                }
            } catch (InterruptedException e) {
                logger.trace("Interrupted waiting for incoming ASTM messages");
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                logger.error("Unhandled exception waiting for incoming ASTM messages", e);
            } finally {
                IdleState.this.outgoingWaiterThread.interrupt();
            }
        }
    };
    Thread incomingWaiterThread;

    private Runnable outgoingWaiter = new Runnable() {
        public final void run() {
            IdleState.this.outgoingSemaphore.release();
            try {
                IdleState.this.incomingSemaphore.acquire();
                IdleState.this.timeoutTimer.reset();
                if (IdleState.this.context.getMessageIterator() == null) {
                    logger.debug("Waiting for new outgoing messages");
                    IdleState.this.context.waitForOutgoingMessage();
                    logger.debug("Outgoing message received for sending");
                } else {
                    logger.debug("Using existing outgoing message for sending");
                }
                logger.debug("Stopped checking outgoing messages");
                return;
            } catch (InterruptedException e) {
                logger.trace("Interrupted while checking outgoing messages");
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                logger.error("Unhandled exception while checking for incoming ASTM messages", e);
            } finally {
                IdleState.this.incomingWaiterThread.interrupt();
            }
        }
    };
    Thread outgoingWaiterThread;

    @Override
    protected final void init() throws IOException {
        super.init();
        this.timeoutTimer = new IdleTimeoutTimer();
        this.timeoutTimer.reset();
        this.receivedEnq = false;
        this.retryCount = 0;
        this.incomingSemaphore = new Semaphore(0);
        this.outgoingSemaphore = new Semaphore(0);
    }

    public IdleState(AstmContext context) throws IOException {
        super(context);
    }

    @Override
    public final String getName() {
        return "Idle";
    }

    @Override
    public final AstmConnectionStatus getStatus() {
        return AstmConnectionStatus.IDLE;
    }

    @Override
    public final void execute() throws IOException, InterruptedException {
        context.getConnection().setSocketTimeout(0);
        this.incomingWaiterThread = new Thread(this.incomingWaiter, 
            Thread.currentThread().getName() + " -> wait incoming");
        this.outgoingWaiterThread = new Thread(this.outgoingWaiter, 
            Thread.currentThread().getName() + " -> wait outgoing");
        this.incomingWaiterThread.start();
        this.outgoingWaiterThread.start();

        try {
            this.incomingWaiterThread.join();
            this.outgoingWaiterThread.join();

            if (getNextState() instanceof IdleState) {
                if (this.receivedEnq) {
                    // BIDIRECTIONAL FIX (A3): the old code NAKed the instrument's
                    // ENQ whenever an outgoing message was pending
                    // ("Rejected incoming ASTM send request..."). Per ASTM E1381
                    // the first sender owns the line: the analyzer grabbed it
                    // first, so we must ACK and receive its transfer. The pending
                    // outgoing message (messageIterator) is left untouched - when
                    // the receiver returns to Idle, the outgoing waiter reuses the
                    // EXISTING iterator ("Using existing outgoing message") and the
                    // host sends its answer turn (order download) immediately after
                    // the analyzer's query. This is exactly the Pentra 400 6.1->6.2,
                    // i-800 TSREQ->TSDWN and D-10 query->answer sequences.
                    logger.debug("Incoming ASTM send request accepted (yielding the line to the peer; "
                        + "pending outgoing message, if any, is sent afterwards)");
                    context.getConnection().writeByte(6); // ACK
                    transitionTo(TransferReceiverState.class);
                } else if (this.timeoutTimer.isExpired() && context.getMessageIterator() != null) {
                    logger.debug("Trying to send message");
                    context.getConnection().setSocketTimeout(15);
                    context.getConnection().writeByte(5); // ENQ
                    if (this.receivedEnq) {
                        handleTransferCollision();
                    } else {
                        try {
                            int response = context.getConnection().readByteDefaultTimeout(15);
                            switch (response) {
                                case 5: // ENQ
                                    handleTransferCollision();
                                    break;
                                case 6: // ACK
                                    transitionTo(TransferSenderState.class);
                                    break;
                                case 21: // NAK
                                    this.timeoutTimer.reset(10);
                                    ++this.retryCount;
                                    break;
                                default:
                                    logger.warn("Unrecognized response to enquiry: " + 
                                        AstmControlChars.name(response));
                            }
                        } catch (TimeoutException e) {
                            logger.warn("Error, sent ENQ but no response after 15 seconds");
                            context.getConnection().writeByte(4); // EOT
                            ++this.retryCount;
                        }
                    }
                }

                if (this.retryCount >= 6) {
                    String msg = "Maximum number of rejections (6) exceeded, message could not be sent.";
                    logger.error(msg);
                    TransmissionResult result = new TransmissionResult(TransmissionResult.Status.TIMEOUT, msg);
                    this.retryCount = 0;
                    context.clearMessageIterator();
                    context.setTransmissionResult(result);
                }
            }
        } catch (InterruptedException e) {
            TransmissionResult result = new TransmissionResult(
                TransmissionResult.Status.INTERRUPTED, 
                "Message sending task interrupted due to termination.");
            context.setTransmissionResult(result);
            boolean joined = false;
            while (!joined) {
                logger.debug("State interrupted, waiting child threads");
                try {
                    this.incomingWaiterThread.interrupt();
                    this.outgoingWaiterThread.interrupt();
                    this.incomingWaiterThread.join();
                    this.outgoingWaiterThread.join();
                    joined = true;
                } catch (InterruptedException ex) {
                    logger.fatal("Interrupted while joining interrupted subthreads", ex);
                    Thread.currentThread().interrupt();
                }
            }
            Thread.currentThread().interrupt();
            return;
        } catch (IOException e) {
            logger.warn("Connection lost, trying to reconnect");
            transitionTo(ReconnectState.class);
        }
    }

    private void handleTransferCollision() {
        this.receivedEnq = false;
        if (context.getConnection().isServer()) {
            logger.warn("Transfer collision detected. Waiting 20 secs before sending ENQ again");
            this.timeoutTimer.reset(20);
        } else {
            logger.warn("Transfer collision detected. Waiting 1 sec before sending ENQ again");
            this.timeoutTimer.reset(1);
        }
    }
}
