package com.bitdreamit.astm.asyncastm.service.connection;

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
    private Semaphore readSemaphore;
    private Semaphore readySemaphore;
    private volatile int lastByte;
    private boolean semaphoreHeld;
    private Runnable readerTask;
    private Thread readerThread;

    public AbstractAstmConnection(Protocol protocol, String charsetName) {
        this.protocol = protocol;
        this.charsetName = charsetName;
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
            try { this.readerThread.join(); } catch (InterruptedException e) { throw new IOException(e); }
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
        Charset charset = Charset.forName(this.charsetName);
        this.getOutputStreamWithReconnect().write(framed.getBytes(charset));
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
                // FIX #8: Mark semaphore as held so next call does not issue a redundant release.
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

    public final String readLine() throws InterruptedException, EOFException {
        StringBuilder sb = new StringBuilder();
        boolean done = false;
        while (!done) {
            int b = this.readByteImpl();
            if (b == 13) {
                b = this.readByteImpl();
                if (b == 10) { done = true; } else { sb.append('\r'); }
            }
            if (!done) {
                Charset charset = Charset.forName(this.charsetName);
                sb.append(new String(new byte[]{(byte) b}, charset));
            }
        }
        return sb.toString();
    }

    public final int readByteDefaultTimeout(int ignored) throws InterruptedException, TimeoutException, EOFException {
        int b = this.readByte(15); logger.trace(AstmControlChars.name(b) + " received"); return b;
    }

    // NEW: Read with explicit timeout, throws TimeoutException on semaphore timeout
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

    {
        this.readerTask = new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        AbstractAstmConnection.this.readSemaphore.acquire();
                        boolean success = false;
                        // Loop internally on SocketTimeoutException so caller gets TimeoutException
                        // from the semaphore layer instead of EOFException.
                        // Note: rare race possible if byte arrives after caller timeout but before
                        // next readSemaphore acquire — the byte may be delivered to next caller.
                        while (!success) {
                            try {
                                AbstractAstmConnection.this.lastByte = AbstractAstmConnection.this.doGetInputStream().read();
                                success = true;
                                AbstractAstmConnection.this.readySemaphore.release();
                            } catch (SocketTimeoutException e) {
                                logger.trace("Timeout reached reading ASTM byte");
                                // Loop again without releasing readySemaphore
                            } catch (IOException e) {
                                logger.error("IOException reading byte", e);
                                AbstractAstmConnection.this.lastByte = -1;
                                success = true;
                                AbstractAstmConnection.this.readySemaphore.release();
                                return; // Exit thread on real IOException
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