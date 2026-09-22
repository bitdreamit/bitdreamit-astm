package com.bitdreamit.astm.asyncastm.service.connection;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;

public abstract class AbstractAstmConnection implements Closeable {
    private static final Logger logger = Logger.getLogger(AbstractAstmConnection.class.getName());

    private Protocol protocol;
    private String charsetName;
    private Charset cachedCharset;
    private Semaphore readSemaphore;
    private Semaphore readySemaphore;
    private volatile int lastByte;
    private boolean semaphoreHeld;
    private Runnable readerTask;
    private Thread readerThread;

    public AbstractAstmConnection(Protocol protocol, String charsetName) {
        this.protocol = protocol;
        this.charsetName = charsetName;
        try {
            this.cachedCharset = Charset.forName(this.charsetName);
        } catch (Exception e) {
            logger.warn("Charset '" + charsetName + "' not found. Falling back to windows-1252 (CP-1252).");
            this.cachedCharset = Charset.forName("windows-1252");
            this.charsetName = "windows-1252";
        }
    }

    public final synchronized void initialize() {
        if (this.readerThread != null && this.readerThread.isAlive()) {
            this.readerThread.interrupt();
            try { this.readerThread.join(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        this.readerThread = new Thread(this.readerTask);
        this.readSemaphore = new Semaphore(0, true);
        this.readySemaphore = new Semaphore(0, true);
        this.semaphoreHeld = false;
        this.lastByte = -1;
        this.readerThread.start();
    }

    @Override
    public synchronized void close() throws IOException {
        if (this.readerThread != null && this.readerThread.isAlive()) {
            this.readerThread.interrupt();
            // BIDIRECTIONAL FIX (Bug #23): bound the join. The old untimed join()
            // could hang forever when a subclass reader was parked in a native
            // blocking read (jSerialComm readBytes), deadlocking Mirth's undeploy
            // queue. 2s is enough once the underlying port/socket is closed; the
            // reader thread is a daemon, so a straggler cannot block shutdown.
            try {
                this.readerThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (this.readerThread.isAlive()) {
                logger.warn("ASTM reader thread did not stop within 2s — continuing shutdown "
                    + "(daemon thread; it exits on the next read/timeout cycle)");
            }
        }
    }

    public final Protocol getProtocol() { return this.protocol; }
    public final String getCharsetName() { return this.charsetName; }

    private OutputStream getOutputStreamWithReconnect() throws IOException, InterruptedException {
        try { return doGetOutputStream(); }
        catch (IOException e) {
            logger.error("Error getting output stream, reconnecting");
            close(); initialize(); return doGetOutputStream();
        }
    }

    protected abstract OutputStream doGetOutputStream() throws IOException;
    protected abstract InputStream doGetInputStream() throws IOException;

    public final void sendFrame(String text) throws IOException, InterruptedException {
        String framed = "\u0002" + text + '\r' + '\n';
        logger.debug("Sending: " + AstmControlChars.format(framed));
        this.getOutputStreamWithReconnect().write(framed.getBytes(this.cachedCharset));
    }

    private int readByteImpl() throws InterruptedException, EOFException {
        try { return readByte(0); }
        catch (TimeoutException e) {
            logger.fatal("Impossible timeout exception even when timeout is disabled", e);
            throw new EOFException(e.getMessage());
        }
    }

    private int readByte(int timeoutSeconds) throws InterruptedException, TimeoutException, EOFException {
        if (this.semaphoreHeld) { this.semaphoreHeld = false; }
        else { this.readSemaphore.release(); }
        try {
            if (timeoutSeconds == 0) { this.readySemaphore.acquire(); }
            else if (!this.readySemaphore.tryAcquire(timeoutSeconds, TimeUnit.SECONDS)) {
                this.semaphoreHeld = true;
                throw new TimeoutException("Timeout exceeded in semaphore");
            }
        } catch (InterruptedException e) {
            this.semaphoreHeld = true;
            throw new InterruptedException(e.toString());
        }
        if (this.lastByte == -1) {
            String msg = "End of stream reached while waiting for byte in ASTM connection.";
            logger.debug(msg); throw new EOFException(msg);
        }
        return this.lastByte;
    }

    /**
     * Reads a line terminated by CR+LF.
     * IMPROVED: Accumulates raw bytes in ByteArrayOutputStream and decodes once at the end.
     * This guarantees integrity for multi-byte encodings (UTF-8) and is equally correct for
     * single-byte encodings like CP-1252 / windows-1252.
     */
    public final String readLine() throws InterruptedException, EOFException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        boolean done = false;

        while (!done) {
            int b = this.readByteImpl();
            if (b == 13) { // CR
                int next = this.readByteImpl();
                if (next == 10) { // LF
                    done = true;
                } else {
                    baos.write(13);
                    baos.write(next);
                }
            } else {
                baos.write(b);
            }
        }
        return new String(baos.toByteArray(), this.cachedCharset);
    }

    public final int readByteDefaultTimeout(int ignored) throws InterruptedException, TimeoutException, EOFException {
        int b = this.readByte(15); logger.trace(AstmControlChars.name(b) + " received"); return b;
    }

    public final int readByteWithTimeout(int seconds) throws InterruptedException, TimeoutException, EOFException {
        int b = this.readByte(seconds); logger.trace(AstmControlChars.name(b) + " received"); return b;
    }

    public final int readByteBlocking() throws InterruptedException, EOFException {
        int b = this.readByteImpl(); logger.trace(AstmControlChars.name(b) + " received"); return b;
    }

    public void writeByte(int value) throws IOException, InterruptedException {
        OutputStream out = this.getOutputStreamWithReconnect(); out.write(value); out.flush();
        logger.trace(AstmControlChars.name(value) + " sent");
    }

    public abstract void doConnect() throws IOException, InterruptedException;
    public abstract InetSocketAddress getAddress();
    public abstract void setSocketTimeout(int seconds) throws SocketException;
    public abstract boolean isServer();

    /**
     * FIX (Bug #19): Check if the underlying connection is still alive.
     * Used by the reader thread to distinguish between "read timeout"
     * (transient — port still open, just no data yet) and "port closed"
     * (permanent — real EOF, reader thread should exit).
     *
     * Default implementation returns true (assumes alive). Subclasses
     * like AstmSerialConnection override this to check serialPort.isOpen().
     */
    public boolean isConnectionAlive() {
        return true;
    }

    {
        this.readerTask = new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        AbstractAstmConnection.this.readSemaphore.acquire();
                        boolean success = false;
                        while (!success) {
                            try {
                                int b = AbstractAstmConnection.this.doGetInputStream().read();
                                // BIDIRECTIONAL FIX (Bug #23): honor interrupt even when the
                                // underlying read is a native blocking call that swallowed it
                                if (Thread.currentThread().isInterrupted()) {
                                    logger.trace("Reader thread interrupted — closing read-byte thread");
                                    AbstractAstmConnection.this.lastByte = -1;
                                    success = true;
                                    AbstractAstmConnection.this.readySemaphore.release();
                                    return;
                                }
                                if (b == -1) {
                                    // FIX (Bug #19): read() returned -1.
                                    // With TIMEOUT_READ_BLOCKING and timeout=0, this means the
                                    // port was closed. But with a non-zero timeout, it could
                                    // also mean "no data within the timeout period."
                                    // Check isConnectionAlive() to distinguish:
                                    if (AbstractAstmConnection.this.isConnectionAlive()) {
                                        // Port is still open — this was just a read timeout.
                                        // Log at TRACE and retry the read.
                                        logger.trace("read() returned -1 but port is still open — treating as timeout, retrying");
                                        continue; // retry the inner while loop
                                    } else {
                                        // Port is closed — real EOF.
                                        logger.debug("read() returned -1 and port is closed — EOF");
                                        AbstractAstmConnection.this.lastByte = -1;
                                        success = true;
                                        AbstractAstmConnection.this.readySemaphore.release();
                                        return;
                                    }
                                }
                                AbstractAstmConnection.this.lastByte = b;
                                success = true;
                                AbstractAstmConnection.this.readySemaphore.release();
                            } catch (SocketTimeoutException e) {
                                logger.trace("Timeout reached reading ASTM byte");
                            } catch (IOException e) {
                                logger.error("IOException reading byte", e);
                                AbstractAstmConnection.this.lastByte = -1;
                                success = true;
                                AbstractAstmConnection.this.readySemaphore.release();
                                return;
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    logger.trace("Closing read-byte thread"); Thread.currentThread().interrupt();
                }
            }
        };
    }
}
