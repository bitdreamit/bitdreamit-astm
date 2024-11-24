package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import java.io.IOException;

public final class h extends i {
   public h(com.bitdreamit.astm.asyncastm.service.states.bundle.a var1) {
      super(var1);
   }

   public final String a() {
      return "Start";
   }

   public final AstmConnectionStatus b() {
      return AstmConnectionStatus.STARTING;
   }

   public final void d() throws IOException {
      this.a(a.class);
   }
}
