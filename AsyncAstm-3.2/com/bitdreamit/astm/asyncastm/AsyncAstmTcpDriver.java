package com.bitdreamit.astm.asyncastm;

import com.bitdreamit.astm.asyncastm.service.states.bundle.a;
import com.bitdreamit.astm.asyncastm.service.connection.Protocol;
import com.bitdreamit.astm.asyncastm.service.connection.b;
import com.bitdreamit.astm.asyncastm.service.connection.d;
import com.bitdreamit.astm.asyncastm.service.connection.e;
import com.bitdreamit.astm.asyncastm.service.states.j;
import com.bitdreamit.astm.asyncastm.service.states.bundle.ReceivedMessage;
import com.bitdreamit.astm.asyncastm.service.states.bundle.TransmissionResult;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmStatusCallback;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;

public class AsyncAstmTcpDriver implements Closeable {
   private static final Logger a = Logger.getLogger(AsyncAstmTcpDriver.class.getName());
   private static List<j> b = new ArrayList();
   private String c;
   private AstmStatusCallback d;
   private j e;
   private int f;
   private String g;
   private int h;
   private String i;

   private static volatile int[] j;

   public int getListeningPort() {
      return this.f;
   }

   public String getBindAddress() {
      return this.g;
   }

   public int getDestinationPort() {
      return this.h;
   }

   public String getDestinationAddress() {
      return this.i;
   }

   public AsyncAstmTcpDriver(String var1) {
      this(var1, new AstmStatusCallback() {
         public final void reportStatus(AstmConnectionStatus var1) {
         }
      });
   }

   public AsyncAstmTcpDriver(String var1, AstmStatusCallback var2) {
      this.c = "";
      this.c = var1;
      this.d = var2;
   }

   public void initiateConnection(String var1, int var2, Protocol var3) throws IOException, LicenseException {
      this.i = var1;
      this.h = var2;
      d var5 = new d(new InetSocketAddress(this.i, this.h), var3, this.c);
      Class var6 = AsyncAstmTcpDriver.class;
      synchronized(AsyncAstmTcpDriver.class) {
         int var7;
         if ((var7 = this.a((b)var5)) == -1) {
            AsyncAstmLicense.a(b.size() + 1);
            a.debug(this.c + ": Creating new client connection");
            this.a(new a(var5));
            b.add(this.e);
         } else {
            a.debug(this.c + ": Reusing existing client connection");
            this.e = (j)b.get(var7);
            this.e.a(this.d);
            this.d.reportStatus(this.e.e());
            this.c = this.e.d().d().c();
         }
      }

      this.e.a();
   }

   public int listenConnections(int var1, String var2, Protocol var3) throws IOException {
      if (var2 == null) {
         var2 = "0.0.0.0";
      }

      this.g = var2;
      e var5 = new e(var1, var2, var3, this.c);
      Class var6 = AsyncAstmTcpDriver.class;
      synchronized(AsyncAstmTcpDriver.class) {
         int var7;
         if ((var7 = this.a((b)var5)) == -1) {
            a.debug(this.c + ": Creating new host connection");
            this.f = var5.l();
            this.a(new a(var5));
            b.add(this.e);
         } else {
            a.debug(this.c + ": Reusing existing host connection");
            this.e = (j)b.get(var7);
            this.e.a(this.d);
            this.d.reportStatus(this.e.e());
            this.c = this.e.d().d().c();
            this.f = this.e.d().d().i().getPort();
         }
      }

      this.e.a();
      return this.f;
   }

   private void a(com.bitdreamit.astm.asyncastm.service.states.bundle.a var1) throws IOException {
      this.e = new j(var1);
      this.e.a(this.d);
      this.e.f();
   }

   private int a(b var1) {
      boolean var2 = false;
      int var3 = 0;

      while(var3 < b.size() && !var2) {
         com.bitdreamit.astm.asyncastm.service.states.bundle.a var4;
         if ((var4 = ((j)b.get(var3)).d()).d().i().equals(var1.i()) && var4.d().j() == var1.j()) {
            if (!var4.d().b().equals(var1.b())) {
               String var6 = this.c + ": There is a previous connection bound to the same address using another protocol (" + var4.d().b() + ")";
               a.fatal(var6);
               throw new IllegalStateException(var6);
            }

            var2 = true;
         } else {
            ++var3;
         }
      }

      return var2 ? var3 : -1;
   }

   public void sendMessage(String var1) throws InterruptedException, TimeoutException, ConnectException {
      TransmissionResult var3 = this.e.d().a(var1);
      this.exceptResult(var3);
   }

   public void sendMessage(String var1, long var2, TimeUnit var4) throws InterruptedException, ConnectException, RejectedExecutionException, TimeoutException {
      TransmissionResult var6 = this.e.d().a(var1, var2, var4);
      this.exceptResult(var6);
   }

   public ReceivedMessage getReceivedMessage() throws InterruptedException {
      return this.e.d().c();
   }

   public ReceivedMessage getReceivedMessage(long var1, TimeUnit var3) throws InterruptedException {
      return this.e.d().a(var1, var3);
   }

   public String getMessage() throws InterruptedException, ConnectException, TimeoutException, RejectedExecutionException {
      ReceivedMessage var1 = this.e.d().c();
      this.exceptResult(var1.getResult());
      return var1.getMessage();
   }

   public String getMessage(long var1, TimeUnit var3) throws InterruptedException,ConnectException, TimeoutException, RejectedExecutionException {
      ReceivedMessage var5;
      if ((var5 = this.e.d().a(var1, var3)) == null) {
         return null;
      } else {
         this.exceptResult(var5.getResult());
         return var5.getMessage();
      }
   }

   public void exceptResult(TransmissionResult var1) throws TimeoutException, ConnectException, InterruptedException, RejectedExecutionException {
      switch(a()[var1.getStatus().ordinal()]) {
      case 1:
         return;
      case 2:
         throw new TimeoutException(var1.getDescription());
      case 3:
         throw new ConnectException(var1.getDescription());
      case 4:
         throw new RejectedExecutionException(var1.getDescription());
      case 5:
         throw new InterruptedException(var1.getDescription());
      case 6:
         throw new RuntimeException(var1.getDescription());
      default:
         throw new RuntimeException(var1.getDescription());
      }
   }

   public synchronized void close() throws IOException {
      if (this.e != null) {
         this.e.b();
         if (this.e.c() == 0) {
            a.info(this.c + ": No more access found, closing ASTM connection");

            try {
               this.e.close();
            } catch (Exception var5) {
               a.fatal(this.c + ": ASTM connection not correctly ended", var5);
            }

            try {
               Class var1 = AsyncAstmTcpDriver.class;
               synchronized(AsyncAstmTcpDriver.class) {
                  int var2;
                  if ((var2 = this.a(this.e.d().d())) != -1) {
                     b.remove(var2);
                     a.debug(this.c + ": Removed state machine from the pool");
                  }
               }
            } catch (NullPointerException var4) {
               a.error("Rare null pointer exception when removing device", var4);
            }
         } else {
            a.info(this.c + ": There are more access, ASTM connection stays open");
         }

         this.e.b(this.d);
      }
   }

   private static int[] a() {
      int[] var10000 = j;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[TransmissionResult.Status.values().length];

         try {
            var0[TransmissionResult.Status.DISCONNECTED.ordinal()] = 3;
         } catch (NoSuchFieldError var6) {
         }

         try {
            var0[TransmissionResult.Status.INTERRUPTED.ordinal()] = 5;
         } catch (NoSuchFieldError var5) {
         }

         try {
            var0[TransmissionResult.Status.REJECTED.ordinal()] = 4;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[TransmissionResult.Status.SUCCESS.ordinal()] = 1;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[TransmissionResult.Status.TIMEOUT.ordinal()] = 2;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[TransmissionResult.Status.UNKNOWN.ordinal()] = 6;
         } catch (NoSuchFieldError var1) {
         }

         j = var0;
         return var0;
      }
   }
}
