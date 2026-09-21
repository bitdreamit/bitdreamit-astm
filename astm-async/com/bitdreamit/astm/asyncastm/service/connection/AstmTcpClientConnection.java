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
 *
 * FIX (Bug #3): initialize() — which starts the reader thread that calls
 * doGetInputStream() — was previously called BEFORE new Socket(...). The
 * reader thread NPE'd on socket.getInputStream() and died silently.
 *
 * FIX (Bug #5): Connection failures were logged at logger.debug() which is
 * below the default Mirth log threshold (INFO). The retry loop ran forever
 * but no error was visible at default log level. Now: first failure at WARN,
 * every 5th failure at ERROR (visible to operators and to Mirth Messages view).
 */
public class AstmTcpClientConnection extends AbstractAstmConnection {
    private static final Logger logger = Logger.getLogger(AstmTcpClientConnection.class.getName());

    /** Hard limit on consecutive connect failures before we give up entirely. */
    private static final int MAX_ATTEMPTS = 60;

    private InetSocketAddress address;
    private Socket socket;
    private boolean connected = false;

    public AstmTcpClientConnection(InetSocketAddress address, Protocol protocol, String charset) {
        super(protocol, charset);
        this.address = address;
    }

    @Override
    public final synchronized void doConnect() throws IOException, InterruptedException {
        if (this.connected) return;
        int attempt = 0;
        int delay = 1000;
        IOException lastError = null;
        logger.info("Connecting to [" + this.address.getHostString()
            + "] on port " + this.address.getPort());
        do {
            try {
                this.socket = new Socket(this.address.getAddress(), this.address.getPort());
                // FIX (Bug #3): initialize the reader thread ONLY AFTER the socket
                // is created. Previously initialize() was called before new Socket(),
                // causing the reader thread to NPE and die silently.
                this.initialize();
                logger.info("Connected to " + this.address.getHostString()
                    + ":" + this.address.getPort() + " successfully");
                this.connected = true;
                return;
            } catch (IOException e) {
                lastError = e;
                ++attempt;
                // FIX (Bug #5): escalate log level so failures are visible at default log level.
                if (attempt == 1) {
                    logger.warn("TCP connect attempt #1 to " + this.address.getHostString()
                        + ":" + this.address.getPort() + " failed: " + e.getMessage()
                        + ". Retrying in " + (delay / 1000) + "s");
                } else if (attempt % 5 == 0) {
                    logger.error("TCP connect attempt #" + attempt + " to "
                        + this.address.getHostString() + ":" + this.address.getPort()
                        + " failed: " + e.getMessage() + ". Retrying in " + (delay / 1000) + "s", e);
                } else {
                    logger.debug("TCP connect attempt #" + attempt + " failed: "
                        + e.getMessage() + ", retry in " + (delay / 1000) + "s");
                }
                if (attempt >= MAX_ATTEMPTS) {
                    throw new IOException("Max TCP connect attempts (" + MAX_ATTEMPTS
                        + ") exceeded connecting to " + this.address.getHostString()
                        + ":" + this.address.getPort(), lastError);
                }
                Thread.sleep(delay);
                if ((delay <<= 1) > 60000) delay = 60000;
            }
        } while (!this.connected);
    }

    @Override
    public final void setSocketTimeout(int seconds) throws SocketException {
        if (this.socket != null) {
            this.socket.setSoTimeout(seconds * 1000);
        }
    }

    @Override
    protected final OutputStream doGetOutputStream() throws IOException {
        if (this.socket == null) {
            throw new IOException("Socket not connected");
        }
        return this.socket.getOutputStream();
    }

    @Override
    protected final InputStream doGetInputStream() throws IOException {
        if (this.socket == null) {
            throw new IOException("Socket not connected");
        }
        return this.socket.getInputStream();
    }

    @Override
    public final boolean isServer() {
        return false;
    }

    @Override
    public synchronized void close() throws IOException {
        if (this.address != null) {
            logger.info("Closing client connection to " + this.address.getHostString()
                + ":" + this.address.getPort());
        }
        if (this.socket != null) {
            this.socket.close();
            this.socket = null;
        }
        super.close();
        this.connected = false;
    }

    public final void writeByteDirect(int value) throws IOException {
        if (this.socket == null) {
            throw new IOException("Socket not connected");
        }
        OutputStream out = this.socket.getOutputStream();
        out.write(value);
        out.flush();
    }

    @Override
    public final InetSocketAddress getAddress() {
        return this.address;
    }

    /**
     * EOF FIX: for TCP sockets an InputStream.read() returning -1 means the
     * PEER CLOSED the connection (socket read timeouts surface as
     * SocketTimeoutException, never as -1). The abstract default (alive=true)
     * made the reader thread spin forever on -1 after an analyzer closed its
     * socket, so the state machine never saw the EOF and never re-armed the
     * listener. TCP connections therefore report "not alive" so read() == -1
     * is correctly treated as EOF.
     */
    @Override
    public boolean isConnectionAlive() {
        return false;
    }
}
