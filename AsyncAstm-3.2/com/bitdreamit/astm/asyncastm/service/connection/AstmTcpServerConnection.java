package com.bitdreamit.astm.asyncastm.service.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import org.apache.log4j.Logger;

/**
 * ASTM TCP Server connection (inbound listener).
 *
 * FIX (Bug #2): initialize() — which starts the background reader thread that
 * calls doGetInputStream() — was previously called BEFORE serverSocket.accept().
 * That meant the reader thread tried to read from clientSocket.getInputStream()
 * while clientSocket was still null → NullPointerException → reader thread died
 * silently → channel appeared Started but never processed any incoming bytes.
 *
 * Now: bind() → accept() → initialize() → connected=true. The reader thread
 * starts only after a real client socket exists.
 */
public class AstmTcpServerConnection extends AbstractAstmConnection {
    private static final Logger logger = Logger.getLogger(AstmTcpServerConnection.class.getName());
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private int port;
    private InetAddress bindAddress;
    private boolean connected = false;

    public AstmTcpServerConnection(int port, String bindAddress, Protocol protocol, String charset) throws IOException {
        super(protocol, charset);
        this.port = port;
        this.bindAddress = InetAddress.getByName(bindAddress);
    }

    public final synchronized int bind() throws IOException {
        if (this.serverSocket == null) {
            this.serverSocket = new ServerSocket(this.port, 50, this.bindAddress);
            this.port = this.serverSocket.getLocalPort();
            logger.info("Listening inbound ASTM on TCP port " + this.serverSocket.getLocalPort()
                + " on " + this.bindAddress.getHostAddress());
        }
        return this.port;
    }

    @Override
    public final void doConnect() throws IOException, InterruptedException {
        if (!this.connected) {
            this.bind();
            // FIX (Bug #2): accept FIRST, then initialize the reader thread.
            // Previously initialize() was called before accept(), causing the
            // reader thread to NPE on clientSocket.getInputStream() and die
            // silently.
            logger.info("Waiting for inbound TCP client to connect on port " + this.port + " ...");
            this.clientSocket = this.serverSocket.accept();
            logger.info("Client [" + this.clientSocket.getInetAddress().getHostAddress()
                + "] connected");
            this.initialize();   // ← now starts AFTER clientSocket exists
            this.connected = true;
        }
    }

    @Override
    public final void setSocketTimeout(int seconds) throws SocketException {
        if (this.clientSocket != null) {
            this.clientSocket.setSoTimeout(seconds * 1000);
        }
    }

    @Override
    protected final OutputStream doGetOutputStream() throws IOException {
        if (this.clientSocket == null) {
            throw new IOException("Client socket not connected");
        }
        return this.clientSocket.getOutputStream();
    }

    @Override
    protected final InputStream doGetInputStream() throws IOException {
        if (this.clientSocket == null) {
            throw new IOException("Client socket not connected");
        }
        return this.clientSocket.getInputStream();
    }

    @Override
    public final boolean isServer() {
        return true;
    }

    @Override
    public synchronized void close() throws IOException {
        if (this.serverSocket != null) {
            this.serverSocket.close();
            this.serverSocket = null;
        }
        if (this.clientSocket != null) {
            this.clientSocket.close();
            this.clientSocket = null;
        }
        super.close();
        this.connected = false;
    }

    @Override
    public final InetSocketAddress getAddress() {
        return new InetSocketAddress(this.bindAddress, this.port);
    }
}
