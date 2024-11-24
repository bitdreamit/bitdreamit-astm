package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import java.io.IOException;

public class c extends i {
   public c(com.bitdreamit.astm.asyncastm.service.states.bundle.a var1) {
      super(var1);
   }

   public final String a() {
      return "Exit";
   }

   public final AstmConnectionStatus b() {
      return AstmConnectionStatus.EXITING;
   }

   public final void d() throws IOException {
   }
}
