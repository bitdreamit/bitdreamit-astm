package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.connection.AstmControlChars;
import com.bitdreamit.astm.asyncastm.service.states.bundle.AstmContext;
import com.bitdreamit.astm.asyncastm.service.states.bundle.ReceivedMessage;
import com.bitdreamit.astm.asyncastm.service.states.bundle.TransmissionResult;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import com.bitdreamit.astm.asyncastm.service.connection.file.FrameBuffer;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;

/**
 * Receives ASTM frames, sends ACK/NAK.
 */
public class TransferReceiverState extends AstmState {
    private static final Logger logger = Logger.getLogger(TransferReceiverState.class.getName());
    private FrameBuffer frameBuffer;
    private boolean badFrameReceived;

    public TransferReceiverState(AstmContext context) throws IOException {
        super(context);
    }

    @Override
    protected final void init() throws IOException {
        super.init();
        this.frameBuffer = new FrameBuffer(context.getConnection().getProtocol());
        this.badFrameReceived = false;
    }

    @Override
    public final String getName() {
        return "Transfer Receiver";
    }

    @Override
    public final AstmConnectionStatus getStatus() {
        return AstmConnectionStatus.RECEIVING;
    }

    @Override
    public final void execute() throws InterruptedException {
        TransmissionResult result;
        String description;
        try {
            context.getConnection().setSocketTimeout(30);
            // FIX: Use readByteWithTimeout instead of readByteBlocking
            int b = context.getConnection().readByteWithTimeout(30);
            switch (b) {
                case 2: // STX
                    try {
                        String frame = context.getConnection().readLine();
                        logger.debug("Frame from ASTM received: " + AstmControlChars.format(frame));
                        frameBuffer.appendFrame(frame);
                        context.getConnection().writeByte(6); // ACK
                        this.badFrameReceived = false;
                        return;
                    } catch (IllegalArgumentException e) {
                        logger.warn("Bad frame received, sending NAK and trying again");
                        this.badFrameReceived = true;
                        context.getConnection().writeByte(21); // NAK
                        return;
                    }
                case 3: // ETX
                default:
                    description = "Illegal start of frame received (" + AstmControlChars.name(b) + "), protocol error";
                    logger.error(description);
                    result = new TransmissionResult(TransmissionResult.Status.REJECTED, description);
                    abortReceiving(result);
                    return;
                case 4: // EOT
                    TransmissionResult finalResult;
                    if (!this.badFrameReceived) {
                        logger.debug("Message received: \n" + frameBuffer.getMessage());
                        finalResult = new TransmissionResult(TransmissionResult.Status.SUCCESS, "Message received successfully");
                    } else {
                        String msg = "Sender has not sent again a malformed part of the message";
                        logger.error(msg);
                        finalResult = new TransmissionResult(TransmissionResult.Status.REJECTED, msg);
                    }
                    ReceivedMessage received = new ReceivedMessage(frameBuffer.getMessage(), finalResult);
                    context.setReceivedMessage(received);
                    transitionTo(IdleState.class);
            }
        } catch (TimeoutException e) {
            // FIX: Catch TimeoutException instead of SocketTimeoutException
            description = "Frame read timeout \n(" + e + ")";
            logger.warn(description);
            result = new TransmissionResult(TransmissionResult.Status.TIMEOUT, description);
            abortReceiving(result);
        } catch (IOException e) {
            description = "Peer disconnected while receiving message";
            logger.error(description, e);
            result = new TransmissionResult(TransmissionResult.Status.DISCONNECTED, description);
            abortReceiving(result);
        }
    }

    private void abortReceiving(TransmissionResult result) throws InterruptedException {
        logger.error("Aborting receiving");
        if (result.getStatus() == TransmissionResult.Status.DISCONNECTED) {
            transitionTo(ReconnectState.class);
        } else {
            try {
                context.getConnection().writeByte(4); // EOT
                transitionTo(IdleState.class);
            } catch (IOException e) {
                logger.error("Disconnected while sending EOT. Trying to reconnect.");
                transitionTo(ReconnectState.class);
            }
        }
        ReceivedMessage received = new ReceivedMessage(frameBuffer.getMessage(), result);
        context.setReceivedMessage(received);
    }
}