package com.bitdreamit.astm.asyncastm.service.states.bundle;

import com.bitdreamit.astm.asyncastm.service.connection.AbstractAstmConnection;
import com.bitdreamit.astm.asyncastm.service.connection.Protocol;
import com.bitdreamit.astm.asyncastm.service.connection.file.MessageIterator;
import com.bitdreamit.astm.asyncastm.service.connection.file.ElecsysMessageIterator;
import com.bitdreamit.astm.asyncastm.service.connection.file.CobasMessageIterator;
import com.bitdreamit.astm.asyncastm.service.states.AstmState;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

public class AstmContext {
    private static final Logger logger = Logger.getLogger(AstmContext.class.getName());
    private AbstractAstmConnection connection;
    private SynchronousQueue<String> outgoingQueue;
    private MessageIterator messageIterator;
    private BlockingQueue<TransmissionResult> resultQueue;
    private BlockingQueue<ReceivedMessage> incomingQueue;
    private List<AstmState> stateCache;
    private static volatile int[] protocolSwitchArray;

    public AstmContext(AbstractAstmConnection connection) {
        this.connection = connection;
        this.outgoingQueue = new SynchronousQueue<>(true);
        this.incomingQueue = new ArrayBlockingQueue<>(1, true);
        initResultQueue(); this.stateCache = new ArrayList<>();
    }
    private void initResultQueue() { this.resultQueue = new ArrayBlockingQueue<>(1, true); }

    public final TransmissionResult sendMessage(String message) throws InterruptedException {
        this.outgoingQueue.put(message); return waitForResult();
    }

    public final void waitForOutgoingMessage() throws InterruptedException {
        String message = this.outgoingQueue.take();
        Protocol protocol = this.connection.getProtocol();
        Object iterator;
        switch (getProtocolSwitchArray()[protocol.ordinal()]) {
            case 1: iterator = new ElecsysMessageIterator(message); break;
            case 2: iterator = new CobasMessageIterator(message); break;
            default: iterator = new CobasMessageIterator(message);
        }
        this.messageIterator = (MessageIterator) iterator;
    }

    public final void setTransmissionResult(TransmissionResult result) {
        BlockingQueue<TransmissionResult> queue = this.resultQueue;
        initResultQueue(); this.messageIterator = null; queue.offer(result);
    }

    private TransmissionResult waitForResult() throws InterruptedException {
        TransmissionResult result;
        try { result = this.resultQueue.take(); }
        catch (InterruptedException e) {
            result = new TransmissionResult(TransmissionResult.Status.INTERRUPTED, "Interruption while waiting sending result");
            Thread.currentThread().interrupt();
        }
        return result;
    }

    public final void setReceivedMessage(ReceivedMessage message) throws InterruptedException {
        this.incomingQueue.put(message);
    }

    // FIX: Check messageIterator instead of the broken hasOutgoing flag.
    // messageIterator is non-null only when we are actively sending a message.
    public final boolean hasOutgoingMessage() {
        return this.messageIterator != null;
    }

    public final ReceivedMessage getReceivedMessage() throws InterruptedException { return this.incomingQueue.take(); }
    public final ReceivedMessage pollReceivedMessage(long timeout, TimeUnit unit) throws InterruptedException { return this.incomingQueue.poll(timeout, unit); }
    public final AbstractAstmConnection getConnection() { return this.connection; }
    public final MessageIterator getMessageIterator() { return this.messageIterator; }
    public final void clearMessageIterator() { this.messageIterator = null; }

    public final AstmState getState(Class<? extends AstmState> stateClass) {
        int i;
        for (i = 0; i < this.stateCache.size() && !this.stateCache.get(i).getClass().equals(stateClass); ++i) {}
        AstmState state;
        if (i < this.stateCache.size()) { state = this.stateCache.get(i); }
        else {
            Class<?>[] paramTypes = new Class[1]; paramTypes[0] = AstmContext.class;
            try {
                state = stateClass.getDeclaredConstructor(paramTypes).newInstance(this);
                this.stateCache.add(state);
            } catch (Exception e) { throw new RuntimeException(e); }
        }
        return state;
    }

    private static int[] getProtocolSwitchArray() {
        int[] arr = protocolSwitchArray; if (arr != null) return arr;
        int[] newArr = new int[Protocol.all().length];
        try { newArr[Protocol.COBAS.ordinal()] = 2; } catch (NoSuchFieldError e) {}
        try { newArr[Protocol.ELECSYS.ordinal()] = 1; } catch (NoSuchFieldError e) {}
        protocolSwitchArray = newArr; return newArr;
    }
}