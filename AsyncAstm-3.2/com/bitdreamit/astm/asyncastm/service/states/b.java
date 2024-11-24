package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.states.bundle.TransmissionResult;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import java.io.IOException;
import org.apache.log4j.Logger;

public class b extends i {
   private static final Logger a = Logger.getLogger(b.class.getName());

   public b(com.bitdreamit.astm.asyncastm.service.states.bundle.a var1) {
      super(var1);
   }

   public final String a() {
      return "Disconnect";
   }

   public final AstmConnectionStatus b() {
      return AstmConnectionStatus.DISCONNECTING;
   }

   public final void d() throws IOException {
      if (this.c.b()) {
         String var1 = "Outgoing message was deleted before being sent due to a disconnection";
         a.error(var1);
         TransmissionResult var2 = new TransmissionResult(TransmissionResult.Status.DISCONNECTED, var1);
         this.c.a(var2);
      }

      this.c.d().close();
      this.a(c.class);
   }
}
