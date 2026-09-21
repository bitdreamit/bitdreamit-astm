package com.bitdreamit.astm.asyncastm;

import com.bitdreamit.astm.asyncastm.service.connection.AbstractAstmConnection;
import com.bitdreamit.astm.asyncastm.service.connection.AstmTcpClientConnection;
import com.bitdreamit.astm.asyncastm.service.connection.AstmTcpServerConnection;
import com.bitdreamit.astm.asyncastm.service.connection.Protocol;
import com.bitdreamit.astm.asyncastm.service.states.AstmStateMachine;
import com.bitdreamit.astm.asyncastm.service.states.bundle.AstmContext;
import com.bitdreamit.astm.asyncastm.service.states.bundle.ReceivedMessage;
import com.bitdreamit.astm.asyncastm.service.states.bundle.TransmissionResult;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmStatusCallback;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;

/**
 * ASTM TCP driver (client or server mode).
 *
 * FIX (Bug #6): isConnected() previously returned true as soon as the state
 * machine object was created — long before any actual socket connection was
 * established. AstmDispatcher.send() relied on this method and would proceed
 * to send to a driver that wasn't actually connected, blocking forever on
 * outgoingQueue.put(). Now isConnected() only returns true when the state
 * machine has reached IDLE (or is actively transferring).
 */
public class AsyncAstmTcpDriver implements AsyncAstmDriver {
    private static final Logger logger = Logger.getLogger(AsyncAstmTcpDriver.class);

    private String name;
    private AstmStatusCallback callback;
    private String bindAddress;
    private int listeningPort;
    private String destinationAddress;
    private int destinationPort;
    private boolean serverMode;
    private Protocol protocol;
    private String charset = "windows-1252";

    private AstmContext context;
    private AstmStateMachine stateMachine;

    // BIDIRECTIONAL FIX (A1/A2): framing configuration propagated from
    // AstmProperties (useChecksum / maxFrameSize) into the AstmContext.
    private int maxFrameSize = 240;
    private boolean checksumEnabled = true;

    // Old constructor (for AstmConnectionManager compatibility)
    public AsyncAstmTcpDriver(String name, AstmStatusCallback callback) {
        this.name = name;
        this.callback = callback;
    }

    // Constructor used by AstmService for TCP client
    public AsyncAstmTcpDriver(String host, int port, boolean serverMode, String protocolStr) {
        this.destinationAddress = host;
        this.destinationPort = port;
        this.serverMode = serverMode;
        this.protocol = parseProtocol(protocolStr);
    }

    // Constructor used by AstmService for TCP server
    public AsyncAstmTcpDriver(int port, boolean serverMode, String protocolStr) {
        this.listeningPort = port;
        this.serverMode = serverMode;
        this.protocol = parseProtocol(protocolStr);
    }

    /**
     * FIX: Tolerant protocol parsing. If the protocol string is null/blank or
     * not a valid Protocol enum value, fall back to ELECSYS rather than throwing
     * IllegalArgumentException and killing the driver instantiation.
     *
     * BIDIRECTIONAL FIX (A1): delegates to Protocol.parse() which also accepts
     * analyzer-family aliases (E1394, D10, PENTRA, ERBA, ...) mapping them to
     * the correct framing mode.
     */
    private static Protocol parseProtocol(String protocolStr) {
        return Protocol.parse(protocolStr);
    }

    /**
     * BIDIRECTIONAL FIX (A1/A2): configure outbound framing and receive-side
     * checksum validation. Called by AstmService.createDriver() with the
     * channel's Max Frame Size and Use Checksum settings; applied to the
     * AstmContext as soon as (or after) the connection is created.
     */
    public void setFrameConfig(int maxFrameSize, boolean checksumEnabled) {
        this.maxFrameSize = (maxFrameSize > 0) ? maxFrameSize : 240;
        this.checksumEnabled = checksumEnabled;
        if (this.context != null) {
            this.context.setMaxFrameContentLength(this.maxFrameSize);
            this.context.setChecksumEnabled(this.checksumEnabled);
        }
    }

    /**
     * Allows AstmReceiver / AstmDispatcher to register a status callback after
     * the driver is constructed via AstmService.createDriver(). This is the key
     * method that enables Bug #4 fix — without it, the driver has no way to
     * report connection state changes back to Mirth.
     */
    public void setStatusCallback(AstmStatusCallback callback) {
        this.callback = callback;
        if (stateMachine != null) {
            stateMachine.addCallback(callback);
        }
    }

    public void setCharset(String charset) {
        if (charset != null && !charset.trim().isEmpty()) {
            this.charset = charset.trim();
        }
    }

    public String getBindAddress() { return bindAddress; }
    public int getListeningPort() { return listeningPort; }
    public String getDestinationAddress() { return destinationAddress; }
    public int getDestinationPort() { return destinationPort; }

    public void listenConnections(int port, String bindAddress, Protocol protocol) {
        this.listeningPort = port;
        this.bindAddress = bindAddress;
        try {
            String bind = (bindAddress != null) ? bindAddress : "0.0.0.0";
            AbstractAstmConnection conn = new AstmTcpServerConnection(port, bind, protocol, charset);
            this.context = new AstmContext(conn);
            this.context.setMaxFrameContentLength(this.maxFrameSize);
            this.context.setChecksumEnabled(this.checksumEnabled);
            this.stateMachine = new AstmStateMachine(context);
            if (callback != null) {
                stateMachine.addCallback(callback);
            }
            stateMachine.start();
            if (callback != null) {
                callback.reportStatus(AstmConnectionStatus.CONNECTING);
            }
            logger.info("Server listening on " + bind + ":" + port);
        } catch (Exception e) {
            throw new RuntimeException("Failed to listen on " + bindAddress + ":" + port, e);
        }
    }

    public void initiateConnection(String host, int port, Protocol protocol) {
        this.destinationAddress = host;
        this.destinationPort = port;
        try {
            AbstractAstmConnection conn = new AstmTcpClientConnection(
                new InetSocketAddress(host, port), protocol, charset);
            this.context = new AstmContext(conn);
            this.context.setMaxFrameContentLength(this.maxFrameSize);
            this.context.setChecksumEnabled(this.checksumEnabled);
            this.stateMachine = new AstmStateMachine(context);
            if (callback != null) {
                stateMachine.addCallback(callback);
            }
            stateMachine.start();
            if (callback != null) {
                callback.reportStatus(AstmConnectionStatus.CONNECTING);
            }
            logger.info("Client connecting to " + host + ":" + port);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initiate connection to " + host + ":" + port, e);
        }
    }

    public void close() {
        try {
            if (stateMachine != null) {
                stateMachine.close();
            }
            if (callback != null) {
                callback.reportStatus(AstmConnectionStatus.DISCONNECTING);
            }
            logger.info("AsyncAstmTcpDriver closed");
        } catch (Exception e) {
            logger.error("Error closing driver", e);
        }
    }

    @Override
    public void start() throws Exception {
        // Auto-initialize from constructor params if not already done
        if (this.context == null) {
            if (this.serverMode) {
                if (this.listeningPort <= 0) {
                    throw new IllegalStateException("Server mode but no listening port configured");
                }
                String bind = (this.bindAddress != null) ? this.bindAddress : "0.0.0.0";
                listenConnections(this.listeningPort, bind, this.protocol);
            } else {
                if (this.destinationAddress == null || this.destinationPort <= 0) {
                    throw new IllegalStateException("Client mode but no destination host/port configured");
                }
                initiateConnection(this.destinationAddress, this.destinationPort, this.protocol);
            }
        }
    }

    @Override
    public void stop() throws Exception {
        close();
    }

    @Override
    public boolean send(byte[] data) throws Exception {
        if (context == null) return false;
        TransmissionResult result = context.sendMessage(new String(data, charset));
        return result.getStatus() == TransmissionResult.Status.SUCCESS;
    }

    @Override
    public byte[] receive() throws Exception {
        return new byte[0];
    }

    /**
     * FIX (Bug #6): Only return true when the state machine has actually
     * reached a state that implies an active connection (IDLE, SENDING,
     * RECEIVING). Previously returned true as soon as the stateMachine object
     * was constructed, which meant AstmDispatcher.send() would proceed before
     * the connection was actually established and block forever on the
     * outgoing queue.
     */
    @Override
    public boolean isConnected() {
        if (stateMachine == null) return false;
        AstmConnectionStatus s = stateMachine.getCurrentStatus();
        return s == AstmConnectionStatus.IDLE
            || s == AstmConnectionStatus.SENDING
            || s == AstmConnectionStatus.RECEIVING;
    }

    @Override
    public ReceivedMessage getReceivedMessage() throws InterruptedException {
        if (context == null) throw new IllegalStateException("Driver not started");
        return context.getReceivedMessage();
    }

    @Override
    public TransmissionResult sendMessage(String message) throws InterruptedException {
        if (context == null) throw new IllegalStateException("Driver not started");
        return context.sendMessage(message);
    }

    /**
     * Force-close the underlying connection. The state machine will then
     * naturally transition to ReconnectState and retry.
     */
    @Override
    public void forceReconnect() {
        try {
            if (context != null && context.getConnection() != null) {
                logger.info("Force-reconnecting TCP driver");
                context.getConnection().close();
            }
        } catch (Exception e) {
            logger.warn("Error during forceReconnect (TCP)", e);
        }
    }
}
