package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import java.io.IOException;

public class g extends i {
   public g(com.bitdreamit.astm.asyncastm.service.states.bundle.a var1) {
      super(var1);
   }

   public final String a() {
      return "Reconnect";
   }

   public final AstmConnectionStatus b() {
      return AstmConnectionStatus.RECONNECTING;
   }

   public final void d() throws IOException {
      try {
         this.c.d().close();
         this.a(a.class);
      } catch (IOException var1) {
         this.a(b.class);
      }
   }
}
