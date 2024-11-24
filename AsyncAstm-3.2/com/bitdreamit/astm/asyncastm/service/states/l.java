package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.states.bundle.ReceivedMessage;
import com.bitdreamit.astm.asyncastm.service.states.bundle.TransmissionResult;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import java.io.IOException;
import java.net.SocketTimeoutException;
import org.apache.log4j.Logger;

public class l extends i {
   private static final Logger a = Logger.getLogger(d.class.getName());
   private com.bitdreamit.astm.asyncastm.service.connection.file.d b;
   private boolean d;

   public l(com.bitdreamit.astm.asyncastm.service.states.bundle.a var1) throws IOException {
      super(var1);
   }

   protected final void c() throws IOException {
      super.c();
      this.b = new com.bitdreamit.astm.asyncastm.service.connection.file.d(this.c.d().b());
      this.d = false;
   }

   public final String a() {
      return "Transfer Receiver";
   }

   public final AstmConnectionStatus b() {
      return AstmConnectionStatus.RECEIVING;
   }

   public final void d() throws InterruptedException {
      TransmissionResult var1;
      String var2;
      try {
         this.c.d().c(30);
         int var6;
         switch(var6 = this.c.d().g()) {
         case 2:
            try {
               var2 = this.c.d().f();
               a.debug("Frame from ASTM received: " + com.bitdreamit.astm.asyncastm.service.connection.a.a(var2));
               this.b.a(var2);
               this.c.d().b(6);
               this.d = false;
               return;
            } catch (IllegalArgumentException var3) {
               a.warn("Bad frame received, sending NAK and trying again");
               this.d = true;
               this.c.d().b(21);
               return;
            }
         case 3:
         default:
            var2 = "Illegal start of frame received(" + com.bitdreamit.astm.asyncastm.service.connection.a.a(var6) + "), protocol error";
            a.error(var2);
            var1 = new TransmissionResult(TransmissionResult.Status.REJECTED, var2);
            this.a(var1);
            return;
         case 4:
            TransmissionResult var9;
            if (!this.d) {
               a.debug("Message received: \n" + this.b.a());
               var9 = new TransmissionResult(TransmissionResult.Status.SUCCESS, "Message received successfully");
            } else {
               String var7 = "Sender has not sent again a malformed part of the message";
               a.error(var7);
               var9 = new TransmissionResult(TransmissionResult.Status.REJECTED, var7);
            }

            ReceivedMessage var8 = new ReceivedMessage(this.b.a(), var9);
            this.c.a(var8);
            this.a(d.class);
         }
      } catch (SocketTimeoutException var4) {
         var2 = "Frame read timeout \n(" + var4 + ")";
         a.warn(var2);
         var1 = new TransmissionResult(TransmissionResult.Status.TIMEOUT, var2);
         this.a(var1);
      } catch (IOException var5) {
         var2 = "Peer disconnected while receiving message";
         a.error(var2, var5);
         var1 = new TransmissionResult(TransmissionResult.Status.DISCONNECTED, var2);
         this.a(var1);
      }
   }

   private void a(TransmissionResult var1) throws InterruptedException {
      a.error("Aborting receiving");
      if (var1.getStatus() == TransmissionResult.Status.DISCONNECTED) {
         this.a(g.class);
      } else {
         try {
            this.c.d().b(4);
            this.a(d.class);
         } catch (IOException var3) {
            a.error("Disconnected while sending EOT. Trying to reconnect.");
            this.a(g.class);
         }
      }

      ReceivedMessage var4 = new ReceivedMessage(this.b.a(), var1);
      this.c.a(var4);
   }
}
