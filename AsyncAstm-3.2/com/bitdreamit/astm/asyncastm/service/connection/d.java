package com.bitdreamit.astm.asyncastm.service.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import org.apache.log4j.Logger;

public class d extends b {
   private static final Logger a = Logger.getLogger(d.class.getName());
   private InetSocketAddress b;
   private Socket c;
   private boolean d = false;

   public d(InetSocketAddress var1, Protocol var2, String var3) {
      super(var2, var3);
      this.b = var1;
   }

   public final synchronized void h() throws InterruptedException {
      if (!this.d) {
         this.a();
         int var1 = 0;
         int var2 = 1000;
         a.info("Connecting to [" + this.b.getHostName() + "] on port " + this.b.getPort());

         do {
            try {
               this.c = new Socket(this.b.getAddress(), this.b.getPort());
               a.info("Connected successfully");
               this.d = true;
            } catch (IOException var3) {
               ++var1;
               a.debug("Connection failed. Trying to reconnect in " + var2 / 1000 + " seconds (" + var1 + ")");
               Thread.sleep((long)var2);
               if ((var2 <<= 1) > 60000) {
                  var2 = 60000;
               }
            }
         } while(!this.d);

      }
   }

   public final void c(int var1) throws SocketException {
      this.c.setSoTimeout(var1 * 1000);
   }

   protected final OutputStream d() throws IOException {
      return this.c.getOutputStream();
   }

   protected final InputStream e() throws IOException {
      return this.c.getInputStream();
   }

   public final boolean j() {
      return false;
   }

   public synchronized void close() throws IOException {
      if (this.b != null) {
         a.info("Closing connections on client connected to [" + this.b.getAddress().getHostName() + "] :" + this.b.getPort());
      }

      if (this.c != null) {
         this.c.close();
      }

      super.close();
      this.d = false;
   }

   public final void b(int var1) throws IOException {
      OutputStream var2;
      (var2 = this.c.getOutputStream()).write(var1);
      var2.flush();
   }

   public final InetSocketAddress i() {
      return this.b;
   }
}
