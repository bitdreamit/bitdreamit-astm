package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmStatusCallback;
import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;

public class j implements Closeable {
   private static final Logger d = Logger.getLogger(j.class.getName());
   private Thread e;
   i a;
   com.bitdreamit.astm.asyncastm.service.states.bundle.a b;
   Set<AstmStatusCallback> c;
   private int f = 0;
   private Runnable g = new Runnable() {
      public final void run() {
         j.this.a = new h(j.this.b);

         try {
            Iterator var2;
            do {
               j.d.debug("Executing state " + j.this.a.a());
               if (j.this.a.h()) {
                  var2 = j.this.c.iterator();

                  while(var2.hasNext()) {
                     ((AstmStatusCallback)var2.next()).reportStatus(j.this.a.b());
                  }
               }

               j.this.a = j.this.a.f();
            } while(!(j.this.a instanceof c));

            j.d.debug("ASTM State machine exiting");
            var2 = j.this.c.iterator();

            while(var2.hasNext()) {
               ((AstmStatusCallback)var2.next()).reportStatus(AstmConnectionStatus.EXITING);
            }

            j.this.a.f();
         } catch (Exception var3) {
            j.d.fatal("Unexpected exception in state machine, aborting execution", var3);
            Iterator var1 = j.this.c.iterator();

            while(var1.hasNext()) {
               ((AstmStatusCallback)var1.next()).reportStatus(AstmConnectionStatus.ERROR);
            }

         }
      }
   };

   public final synchronized void a() {
      ++this.f;
      d.debug("StateMachine access added (" + this.f + " access)");
   }

   public final synchronized void b() {
      if (this.f != 0) {
         --this.f;
         d.debug("StateMachine access removed (" + this.f + " access)");
      } else {
         d.debug("There weren't any access already");
      }
   }

   public final synchronized int c() {
      return this.f;
   }

   public j(com.bitdreamit.astm.asyncastm.service.states.bundle.a var1) {
      this.b = var1;
      this.c = ConcurrentHashMap.newKeySet();
   }

   public final void a(AstmStatusCallback var1) {
      this.c.add(var1);
   }

   public final void b(AstmStatusCallback var1) {
      this.c.remove(var1);
   }

   public final com.bitdreamit.astm.asyncastm.service.states.bundle.a d() {
      return this.b;
   }

   public final AstmConnectionStatus e() {
      return this.a == null ? AstmConnectionStatus.STARTING : this.a.b();
   }

   public final void f() throws IOException {
      this.e = new Thread(this.g);
      this.e.start();
   }

   public void close() throws IOException {
      d.debug("Interrupting state machine thread");
      this.e.interrupt();

      try {
         this.a.close();
      } catch (NullPointerException var3) {
      }

      try {
         this.e.join(10000L);
         if (this.e.isAlive()) {
            d.error("Thread still alive, retrying to close");
            this.b.d().close();
            int var1 = 0;

            while(this.e.isAlive()) {
               d.debug("Stopping ASTM connection");
               this.e.interrupt();
               this.e.join(1000L);
               ++var1;
               if (var1 % 10 == 0) {
                  d.debug("Thread not stopped after " + var1 + " retries");
                  this.b.d().close();
               }
            }

         }
      } catch (InterruptedException var2) {
         throw new IOException(var2);
      }
   }
}
