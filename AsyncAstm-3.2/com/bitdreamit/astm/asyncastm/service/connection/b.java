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

public abstract class b implements Closeable {
   private static final Charset a = Charset.forName("windows-1252");
   private static final Logger b = Logger.getLogger(b.class.getName());
   private Protocol c;
   private String d;
   private Semaphore e;
   private Semaphore f;
   private volatile int g;
   private boolean h;
   private Runnable i = new Runnable() {
      public final void run() {
         try {
            while(true) {
               b.this.e.acquire();

               try {
                  b.this.g = b.this.e().read();
                  continue;
               } catch (SocketTimeoutException var6) {
                  com.bitdreamit.astm.asyncastm.service.connection.b.b.trace("Timeout reached reading ASTM byte");
                  b.this.g = -1;
                  return;
               } catch (IOException var7) {
                  b.this.g = -1;
               } finally {
                  b.this.f.release();
               }

               return;
            }
         } catch (InterruptedException var9) {
            com.bitdreamit.astm.asyncastm.service.connection.b.b.trace("Closing readbyte thread");
            Thread.currentThread().interrupt();
         }
      }
   };
   private Thread j;

   public b(Protocol var1, String var2) {
      this.c = var1;
      this.d = var2;
   }

   public final synchronized void a() {
      this.j = new Thread(this.i);
      this.e = new Semaphore(0, true);
      this.f = new Semaphore(0, true);
      this.h = false;
      this.g = -1;
      this.j.start();
   }

   public synchronized void close() throws IOException {
      if (this.j != null && this.j.isAlive()) {
         this.j.interrupt();

         try {
            this.j.join();
         } catch (InterruptedException var2) {
            throw new IOException(var2);
         }
      }
   }

   public final Protocol b() {
      return this.c;
   }

   public final String c() {
      return this.d;
   }

   private OutputStream l() throws IOException, InterruptedException {
      try {
         return this.d();
      } catch (IOException var1) {
         b.error("Error getting output stream, reconnecting");
         this.a();
         return this.d();
      }
   }

   protected abstract OutputStream d() throws IOException;

   protected abstract InputStream e() throws IOException;

   public final void a(String var1) throws IOException, InterruptedException {
      var1 = "\u0002" + var1 + '\r' + '\n';
      b.debug("Sending: " + com.bitdreamit.astm.asyncastm.service.connection.a.a(var1));
      this.l().write(var1.getBytes(a));
   }

   private int m() throws InterruptedException, EOFException {
      try {
         return this.d(0);
      } catch (TimeoutException var2) {
         b.fatal("Impossible timeout exception even when the timeout is disabled", var2);
         throw new EOFException(var2.getMessage());
      }
   }

   private int d(int var1) throws InterruptedException, TimeoutException, EOFException {
      if (this.h) {
         this.h = false;
      } else {
         this.e.release();
      }

      try {
         if (var1 == 0) {
            this.f.acquire();
         } else if (!this.f.tryAcquire((long)var1, TimeUnit.SECONDS)) {
            throw new TimeoutException("Timeout exceeded in semaphore");
         }
      } catch (InterruptedException var2) {
         this.h = true;
         throw new InterruptedException(var2.toString());
      }

      if (this.g == -1) {
         String var3 = "End of stream reached while waiting for byte in ASTM connection.";
         b.debug(var3);
         throw new EOFException(var3);
      } else {
         return this.g;
      }
   }

   public final String f() throws InterruptedException, EOFException {
      StringBuilder var1 = new StringBuilder();
      boolean var2 = false;

      while(!var2) {
         int var3;
         if ((var3 = this.m()) == 13) {
            if ((var3 = this.m()) == 10) {
               var2 = true;
            } else {
               var1.append('\r');
            }
         }

         if (!var2) {
            var1.append(new String(new byte[]{(byte)var3}, a));
         }
      }

      return var1.toString();
   }

   public final int a(int var1) throws InterruptedException, TimeoutException, EOFException {
      var1 = this.d(15);
      b.trace(com.bitdreamit.astm.asyncastm.service.connection.a.a(var1) + " received");
      return var1;
   }

   public final int g() throws InterruptedException, EOFException {
      int var1 = this.m();
      b.trace(com.bitdreamit.astm.asyncastm.service.connection.a.a(var1) + " received");
      return var1;
   }

   public void b(int var1) throws IOException, InterruptedException {
      OutputStream var2;
      (var2 = this.l()).write(var1);
      var2.flush();
      b.trace(com.bitdreamit.astm.asyncastm.service.connection.a.a(var1) + " sent");
   }

   public abstract void h() throws IOException, InterruptedException;

   public abstract InetSocketAddress i();

   public abstract void c(int var1) throws SocketException;

   public abstract boolean j();
}
