package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.states.bundle.TransmissionResult;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import java.io.IOException;
import java.net.SocketTimeoutException;
import org.apache.log4j.Logger;

public class m extends i {
   private static final Logger a = Logger.getLogger(m.class.getName());
   private int b;
   private String d;
   private int e;

   public m(com.bitdreamit.astm.asyncastm.service.states.bundle.a var1) throws IOException {
      super(var1);
   }

   protected final void c() throws IOException {
      super.c();
      this.b = 0;
      this.e = 0;
      this.d = null;
      this.c.d().c(15);
   }

   public final String a() {
      return "Transfer Sender";
   }

   public final AstmConnectionStatus b() {
      return AstmConnectionStatus.SENDING;
   }

   public final void d() throws IOException, InterruptedException {
      String var1;
      if (this.b == 6) {
         var1 = "Reached limit of retries for sending frame 0";
         a.error(var1);
         this.a(TransmissionResult.Status.REJECTED, var1);
      } else {
         if (this.d == null) {
            if (!this.c.e().hasNext()) {
               a.debug("Message sent sucessfully");
               this.c.d().b(4);
               this.c.f();
               TransmissionResult var2 = new TransmissionResult(TransmissionResult.Status.SUCCESS, "Message sent successfully");
               this.c.a(var2);
               this.a(d.class);
               return;
            }

            this.d = this.c.e().b();
         }

         this.c.d().a(this.d);

         try {
            switch(this.c.d().g()) {
            case 4:
               var1 = "Remote device has requested to halt message sending";
               a.error(var1);
               this.a(TransmissionResult.Status.REJECTED, var1);
               return;
            case 5:
            default:
               ++this.b;
               return;
            case 6:
               this.d = null;
            }
         } catch (SocketTimeoutException var3) {
            var1 = "Error, sent ENQ but no response after 15 second(s)";
            a.error(var1);
            this.a(TransmissionResult.Status.TIMEOUT, var1);
         } catch (IOException var4) {
            var1 = "Peer disconnected while sending message";
            a.error(var1);
            this.a(TransmissionResult.Status.DISCONNECTED, var1);
         }
      }
   }

   private void a(TransmissionResult.Status var1, String var2) throws IOException, InterruptedException {
      a.error("Aborting transfer");
      if (var1 == TransmissionResult.Status.DISCONNECTED) {
         this.a(g.class);
      } else {
         this.c.d().b(4);
         this.a(d.class);
      }

      TransmissionResult var4 = new TransmissionResult(var1, "Error ocurred while sending message, aborting transfer. \n(" + var2 + ").");
      this.c.a(var4);
   }
}
