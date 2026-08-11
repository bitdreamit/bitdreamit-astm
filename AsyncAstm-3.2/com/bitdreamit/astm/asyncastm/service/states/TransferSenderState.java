package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.states.bundle.AstmContext;
import com.bitdreamit.astm.asyncastm.service.states.bundle.TransmissionResult;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;

/**
 * Sends ASTM frames, waits for ACK/NAK.
 */
public class TransferSenderState extends AstmState {
    private static final Logger logger = Logger.getLogger(TransferSenderState.class.getName());
    private int retryCount;
    private String currentFrame;
    private int frameIndex;

    public TransferSenderState(AstmContext context) throws IOException {
        super(context);
    }

    @Override
    protected final void init() throws IOException {
        super.init();
        this.retryCount = 0;
        this.frameIndex = 0;
        this.currentFrame = null;
        context.getConnection().setSocketTimeout(15);
    }

    @Override
    public final String getName() {
        return "Transfer Sender";
    }

    @Override
    public final AstmConnectionStatus getStatus() {
        return AstmConnectionStatus.SENDING;
    }

    @Override
    public final void execute() throws IOException, InterruptedException {
        String description;
        if (this.retryCount == 6) {
            description = "Reached limit of retries for sending frame 0";
            logger.error(description);
            abortSending(TransmissionResult.Status.REJECTED, description);
        } else {
            if (this.currentFrame == null) {
                if (!context.getMessageIterator().hasNext()) {
                    logger.debug("Message sent successfully");
                    context.getConnection().writeByte(4); // EOT
                    context.clearMessageIterator();
                    TransmissionResult result = new TransmissionResult(TransmissionResult.Status.SUCCESS, "Message sent successfully");
                    context.setTransmissionResult(result);
                    transitionTo(IdleState.class);
                    return;
                }
                this.currentFrame = context.getMessageIterator().nextFrame();
            }

            context.getConnection().sendFrame(this.currentFrame);

            try {
                // FIX: Use readByteWithTimeout instead of readByteBlocking
                int response = context.getConnection().readByteWithTimeout(15);
                switch (response) {
                    case 4: // EOT
                        description = "Remote device has requested to halt message sending";
                        logger.error(description);
                        abortSending(TransmissionResult.Status.REJECTED, description);
                        return;
                    case 5: // ENQ
                    default:
                        ++this.retryCount;
                        return;
                    case 6: // ACK
                        this.currentFrame = null;
                }
            } catch (TimeoutException e) {
                // FIX: Catch TimeoutException instead of SocketTimeoutException
                description = "Error, sent frame but no response after 15 second(s)";
                logger.error(description);
                abortSending(TransmissionResult.Status.TIMEOUT, description);
            } catch (IOException e) {
                description = "Peer disconnected while sending message";
                logger.error(description);
                abortSending(TransmissionResult.Status.DISCONNECTED, description);
            }
        }
    }

    private void abortSending(TransmissionResult.Status status, String description) throws IOException, InterruptedException {
        logger.error("Aborting transfer");
        if (status == TransmissionResult.Status.DISCONNECTED) {
            transitionTo(ReconnectState.class);
        } else {
            context.getConnection().writeByte(4); // EOT
            transitionTo(IdleState.class);
        }
        TransmissionResult result = new TransmissionResult(status,
                "Error occurred while sending message, aborting transfer. \n(" + description + ").");
        context.setTransmissionResult(result);
    }
}