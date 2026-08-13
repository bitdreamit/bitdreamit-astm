package com.bitdreamit.astm.asyncastm.service.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import org.apache.log4j.Logger;

/**
 * ASTM TCP Client connection (outbound).
 */
public class AstmTcpClientConnection extends AbstractAstmConnection {
    private static final Logger logger = Logger.getLogger(AstmTcpClientConnection.class.getName());

    private InetSocketAddress address;
    private Socket socket;
    private boolean connected = false;

    public AstmTcpClientConnection(InetSocketAddress address, Protocol protocol, String charset) {
        super(protocol, charset);
        this.address = address;
    }

    @Override
    public final synchronized void doConnect() throws InterruptedException {
        if (this.connected) return;
        this.initialize();
        int attempt = 0;
        int delay = 1000;
        logger.info("Connecting to [" + this.address.getHostName() + "] on port " + this.address.getPort());
        do {
            try {
                this.socket = new Socket(this.address.getAddress(), this.address.getPort());
                logger.info("Connected successfully to " + this.address.getHostName() + ":" + this.address.getPort());
                this.connected = true;
            } catch (IOException e) {
                ++attempt;
                // FIX: log at WARN (not DEBUG) so users can actually see why
                // the channel appears "started but silent". Previously DEBUG
                // is hidden at default Mirth INFO log level.
                logger.warn("TCP connect attempt " + attempt + " failed to "
                        + this.address.getHostName() + ":" + this.address.getPort()
                        + " (" + e.getClass().getSimpleName() + ": " + e.getMessage()
                        + "). Retrying in " + (delay / 1000) + "s.");
                Thread.sleep(delay);
                if ((delay <<= 1) > 60000) delay = 60000;
            }
        } while (!this.connected);
    }

    @Override
    public final void setSocketTimeout(int seconds) throws SocketException {
        this.socket.setSoTimeout(seconds * 1000);
    }

    @Override
    protected final OutputStream doGetOutputStream() throws IOException {
        return this.socket.getOutputStream();
    }

    @Override
    protected final InputStream doGetInputStream() throws IOException {
        return this.socket.getInputStream();
    }

    @Override
    public final boolean isServer() {
        return false;
    }

    @Override
    public synchronized void close() throws IOException {
        if (this.address != null) {
            logger.info("Closing client connection to [" + this.address.getAddress().getHostName() + "]:" + this.address.getPort());
        }
        if (this.socket != null) this.socket.close();
        super.close();
        this.connected = false;
    }

    public final void writeByteDirect(int value) throws IOException {
        OutputStream out = this.socket.getOutputStream();
        out.write(value);
        out.flush();
    }

    @Override
    public final InetSocketAddress getAddress() {
        return this.address;
    }
}
