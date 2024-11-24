package com.bitdreamit.astm.asyncastm.service.states.bundle;

import com.bitdreamit.astm.asyncastm.service.connection.file.b;
import com.bitdreamit.astm.asyncastm.service.connection.Protocol;
import com.bitdreamit.astm.asyncastm.service.connection.file.c;
import com.bitdreamit.astm.asyncastm.service.states.i;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

public class a {
   private static final Logger a = Logger.getLogger(a.class.getName());
   private com.bitdreamit.astm.asyncastm.service.connection.b b;
   private SynchronousQueue<String> c;
   private c d;
   private BlockingQueue<TransmissionResult> e;
   private volatile boolean f = true;
   private BlockingQueue<ReceivedMessage> g;
   private List<i> h;
   private static volatile int[] i;

   public a(com.bitdreamit.astm.asyncastm.service.connection.b var1) {
      this.b = var1;
      this.c = new SynchronousQueue(true);
      this.g = new ArrayBlockingQueue(1, true);
      this.g();
      this.h = new ArrayList();
   }

   public final TransmissionResult a(String var1) throws InterruptedException {
      this.c.put(var1);
      return this.h();
   }

   public final TransmissionResult a(String var1, long var2, TimeUnit var4) throws InterruptedException {
      TransmissionResult var5;
      if (this.c.offer(var1, var2, var4)) {
         var5 = this.h();
      } else {
         var5 = new TransmissionResult(TransmissionResult.Status.TIMEOUT, "Timeout trying to send message");
      }

      return var5;
   }

   private void g() {
      this.e = new ArrayBlockingQueue(1, true);
   }

   public final void a() throws InterruptedException {
      String var1 = (String)this.c.take();
      String var2 = var1;
      Protocol var3 = this.b.b();
      Object var10001;
      switch(i()[var3.ordinal()]) {
      case 1:
         var10001 = new b(var2);
         break;
      case 2:
         var10001 = new com.bitdreamit.astm.asyncastm.service.connection.file.a(var2);
         break;
      default:
         a.fatal("There is no iterator defined for protocol '" + var3.toString() + "'. Defaulting to COBAS");
         var10001 = new com.bitdreamit.astm.asyncastm.service.connection.file.a(var2);
      }

      this.d = (c)var10001;
   }

   public final void a(TransmissionResult var1) {
      BlockingQueue var2 = this.e;
      this.g();
      this.d = null;
      var2.offer(var1);
   }

   private TransmissionResult h() throws InterruptedException {
      TransmissionResult var1;
      try {
         var1 = (TransmissionResult)this.e.take();
      } catch (InterruptedException var2) {
         var1 = new TransmissionResult(TransmissionResult.Status.INTERRUPTED, "Interruption while waiting sending result");
         Thread.currentThread().interrupt();
      }

      return var1;
   }

   public final void a(ReceivedMessage var1) throws InterruptedException {
      this.f = false;
      this.g.put(var1);
   }

   public final boolean b() {
      return !this.f;
   }

   public final ReceivedMessage c() throws InterruptedException {
      ReceivedMessage var1 = (ReceivedMessage)this.g.take();
      this.f = true;
      return var1;
   }

   public final ReceivedMessage a(long var1, TimeUnit var3) throws InterruptedException {
      ReceivedMessage var4;
      if ((var4 = (ReceivedMessage)this.g.poll(var1, var3)) != null) {
         this.f = true;
      }

      return var4;
   }

   public final com.bitdreamit.astm.asyncastm.service.connection.b d() {
      return this.b;
   }

   public final c e() {
      return this.d;
   }

   public final void f() {
      this.d = null;
   }

   public final i a(Class<? extends i> var1) {
      int var2;
      for(var2 = 0; var2 < this.h.size() && !((i)this.h.get(var2)).getClass().equals(var1); ++var2) {
      }

      i var4;
      if (var2 < this.h.size()) {
         var4 = (i)this.h.get(var2);
         a.debug("Setting already created state " + var4.a());
      } else {
         Class[] var5;
         (var5 = new Class[1])[0] = a.class;

         try {
            var4 = (i)var1.getDeclaredConstructor(var5).newInstance(this);
            this.h.add(var4);
            a.debug("Setting new state " + var4.a());
         } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | InstantiationException var3) {
            throw new RuntimeException(var3);
         }
      }

      return var4;
   }

   private static int[] i() {
      int[] var10000 = i;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[Protocol.a().length];

         try {
            var0[Protocol.COBAS.ordinal()] = 2;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[Protocol.ELECSYS.ordinal()] = 1;
         } catch (NoSuchFieldError var1) {
         }

         i = var0;
         return var0;
      }
   }
}
