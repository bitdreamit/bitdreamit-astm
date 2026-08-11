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

public class AstmTcpServerConnection extends AbstractAstmConnection {
    private static final Logger logger = Logger.getLogger(AstmTcpServerConnection.class.getName());
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private int port;
    private InetAddress bindAddress;
    private boolean connected = false;

    public AstmTcpServerConnection(int port, String bindAddress, Protocol protocol, String charset) throws IOException {
        super(protocol, charset); this.port = port; this.bindAddress = InetAddress.getByName(bindAddress);
    }

    public final synchronized int bind() throws IOException {
        if (this.serverSocket == null) {
            this.serverSocket = new ServerSocket(this.port, 50, this.bindAddress);
            this.port = this.serverSocket.getLocalPort();
            logger.info("Listening inbound ASTM on TCP port " + this.serverSocket.getLocalPort());
        }
        return this.port;
    }

    @Override
    public final void doConnect() throws IOException {
        if (!this.connected) {
            this.bind();
            this.initialize();
            // FIX: Do NOT catch SocketException/NullPointerException silently.
            // Allow exceptions to propagate so state machine can transition to ReconnectState.
            this.clientSocket = this.serverSocket.accept();
            logger.info("Client [" + this.clientSocket.getInetAddress().getHostName() + "] connected");
            this.connected = true;
        }
    }

    @Override
    public final void setSocketTimeout(int seconds) throws SocketException {
        if (this.clientSocket != null) {
            this.clientSocket.setSoTimeout(seconds * 1000);
        }
    }

    @Override protected final OutputStream doGetOutputStream() throws IOException { return this.clientSocket.getOutputStream(); }
    @Override protected final InputStream doGetInputStream() throws IOException { return this.clientSocket.getInputStream(); }
    @Override public final boolean isServer() { return true; }

    @Override
    public synchronized void close() throws IOException {
        if (this.serverSocket != null) { this.serverSocket.close(); this.serverSocket = null; }
        if (this.clientSocket != null) this.clientSocket.close();
        super.close(); this.connected = false;
    }

    @Override public final InetSocketAddress getAddress() { return new InetSocketAddress(this.bindAddress, this.port); }
}