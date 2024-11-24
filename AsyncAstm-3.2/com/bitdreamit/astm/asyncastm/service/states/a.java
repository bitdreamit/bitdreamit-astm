package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import java.io.IOException;
import java.net.SocketException;

public class a extends i {
   public a(com.bitdreamit.astm.asyncastm.service.states.bundle.a var1) {
      super(var1);
   }

   public final String a() {
      return "Connect";
   }

   public final AstmConnectionStatus b() {
      return AstmConnectionStatus.CONNECTING;
   }

   protected final void c() throws SocketException, IOException {
      super.c();
   }

   public final void d() throws IOException, InterruptedException {
      try {
         this.c.d().h();
         this.a(d.class);
      } catch (InterruptedException var1) {
         this.a(b.class);
      }
   }

   public void close() throws IOException {
      super.close();
      this.c.d().close();
   }
}
