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

public class e extends b {
   private static final Logger a = Logger.getLogger(e.class.getName());
   private ServerSocket b;
   private Socket c;
   private int d;
   private InetAddress e;
   private boolean f = false;

   public e(int var1, String var2, Protocol var3, String var4) throws IOException {
      super(var3, var4);
      this.d = var1;
      this.e = InetAddress.getByName(var2);
   }

   public final synchronized int l() throws IOException {
      if (this.b == null) {
         this.b = new ServerSocket(this.d, 50, this.e);
         this.d = this.b.getLocalPort();
         a.info("Listening inbound ASTM connections in host on TCP port " + this.b.getLocalPort());
      }

      return this.d;
   }

   public final void h() throws IOException {
      if (!this.f) {
         this.l();
         this.a();

         try {
            this.c = this.b.accept();
            a.info("Client [" + this.c.getInetAddress().getHostName() + "] connected successfully");
            this.f = true;
         } catch (NullPointerException | SocketException var1) {
         }
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
      return true;
   }

   public synchronized void close() throws IOException {
      if (this.b != null) {
         a.info("Closing connections on host listening on port " + this.b.getLocalPort());
         this.b.close();
         this.b = null;
      }

      if (this.c != null) {
         this.c.close();
      }

      super.close();
      this.f = false;
   }

   public final InetSocketAddress i() {
      return new InetSocketAddress(this.e, this.d);
   }
}
