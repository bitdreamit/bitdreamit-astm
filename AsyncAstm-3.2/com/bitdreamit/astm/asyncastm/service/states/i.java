package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import org.apache.log4j.Logger;

public abstract class i implements Closeable {
   private static final Logger a = Logger.getLogger(i.class.getName());
   private i b;
   protected com.bitdreamit.astm.asyncastm.service.states.bundle.a c;
   private boolean d;

   public abstract String a();

   public abstract AstmConnectionStatus b();

   public i(com.bitdreamit.astm.asyncastm.service.states.bundle.a var1) {
      this.c = var1;
      this.d = true;
   }

   protected abstract void d() throws IOException, InterruptedException;

   protected void c() throws SocketException, IOException {
      Thread.currentThread().setName(this.a() + " @ " + this.c.d().c());
      this.b = this;
   }

   public final i f() throws IOException {
      if (this.d) {
         this.c();
         this.d = false;
      }

      try {
         if (Thread.interrupted() && !(this.b instanceof b)) {
            throw new InterruptedException();
         }

         this.d();
      } catch (InterruptedException var1) {
         a.debug("Interrupted state, setting next state to Disconnect");
         this.a(b.class);
      } catch (EOFException var2) {
         a.debug("ASTM disconnection");
         this.a(g.class);
      }

      return this.b;
   }

   public final synchronized void a(Class<? extends i> var1) {
      this.b = this.c.a(var1);
      this.b.d = true;
   }

   public final synchronized i g() {
      return this.b;
   }

   final boolean h() {
      return this.d;
   }

   public void close() throws IOException {
   }
}
