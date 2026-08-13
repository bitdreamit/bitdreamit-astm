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
import java.util.concurrent.TimeUnit;

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
     * FIX: Validate the protocol string and emit a clear error message before
     * it becomes a confusing IllegalArgumentException deep in the constructor.
     * Also trims and uppercases the input.
     */
    private static Protocol parseProtocol(String protocolStr) {
        if (protocolStr == null) {
            throw new IllegalArgumentException("ASTM protocol is null. Expected ELECSYS or COBAS.");
        }
        String trimmed = protocolStr.trim().toUpperCase();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("ASTM protocol is empty. Expected ELECSYS or COBAS.");
        }
        try {
            return Protocol.valueOf(trimmed);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Unknown ASTM protocol: '" + protocolStr + "'. Valid values: ELECSYS, COBAS.", e);
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
            AbstractAstmConnection conn = new AstmTcpServerConnection(port, bindAddress, protocol, charset);
            this.context = new AstmContext(conn);
            this.stateMachine = new AstmStateMachine(context);
            if (callback != null) stateMachine.addCallback(callback);
            stateMachine.start();
            if (callback != null) {
                callback.reportStatus(AstmConnectionStatus.CONNECTING);
            }
            logger.info("Server listening on " + bindAddress + ":" + port);
        } catch (Exception e) {
            // FIX: log at ERROR and rethrow with a clear message
            logger.error("Failed to listen on " + bindAddress + ":" + port + " - " + e.getMessage(), e);
            throw new RuntimeException("Failed to listen on " + bindAddress + ":" + port + " - " + e.getMessage(), e);
        }
    }

    public void initiateConnection(String host, int port, Protocol protocol) {
        this.destinationAddress = host;
        this.destinationPort = port;
        try {
            AbstractAstmConnection conn = new AstmTcpClientConnection(new InetSocketAddress(host, port), protocol, charset);
            this.context = new AstmContext(conn);
            this.stateMachine = new AstmStateMachine(context);
            if (callback != null) stateMachine.addCallback(callback);
            stateMachine.start();
            if (callback != null) {
                callback.reportStatus(AstmConnectionStatus.CONNECTING);
            }
            logger.info("Client connecting to " + host + ":" + port);
        } catch (Exception e) {
            logger.error("Failed to connect to " + host + ":" + port + " - " + e.getMessage(), e);
            throw new RuntimeException("Failed to connect to " + host + ":" + port + " - " + e.getMessage(), e);
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
        // FIX: Auto-initialize from constructor params if not already done
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

    @Override
    public boolean isConnected() {
        return stateMachine != null && stateMachine.getCurrentStatus() != null;
    }

    @Override
    public ReceivedMessage getReceivedMessage() throws InterruptedException {
        if (context == null) throw new IllegalStateException("Driver not started");
        return context.getReceivedMessage();
    }

    @Override
    public ReceivedMessage pollReceivedMessage(long timeout, TimeUnit unit) throws InterruptedException {
        if (context == null) throw new IllegalStateException("Driver not started");
        return context.pollReceivedMessage(timeout, unit);
    }

    @Override
    public TransmissionResult sendMessage(String message) throws InterruptedException {
        if (context == null) throw new IllegalStateException("Driver not started");
        return context.sendMessage(message);
    }

    /**
     * FIX: Allow AstmService to register a callback BEFORE start() is called.
     * Without this, the state machine's callbacks set stays empty and Mirth
     * never receives any state events.
     */
    @Override
    public void addCallback(AstmStatusCallback callback) {
        // Replace if already set (idempotent — safe to call multiple times)
        this.callback = callback;
        if (this.stateMachine != null) {
            this.stateMachine.addCallback(callback);
        }
    }

    /**
     * FIX: Delegates to the state machine. AstmReceiverService polls this to
     * detect a dead driver and stop blocking.
     */
    @Override
    public boolean isAlive() {
        return stateMachine != null && stateMachine.isAlive();
    }
}
