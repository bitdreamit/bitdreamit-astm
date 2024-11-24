package com.bitdreamit.astm.asyncastm.service.states;

import com.bitdreamit.astm.asyncastm.service.states.bundle.TransmissionResult;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;

public class d extends i {
   private static final Logger d = Logger.getLogger(d.class.getName());
   private volatile boolean e;
   private com.bitdreamit.astm.asyncastm.service.states.file.a f;
   private int g;
   private Semaphore h;
   private Semaphore i;
   private Runnable j = new Runnable() {
      public final void run() {
         d.this.h.release();

         try {
            d.this.i.acquire();

            while(true) {
               if (!d.this.e) {
                  com.bitdreamit.astm.asyncastm.service.states.d.d.debug("Waiting for incoming messages");

                  try {
                     int var1;
                     if ((var1 = d.this.c.d().g()) == 5) {
                        com.bitdreamit.astm.asyncastm.service.states.d.d.debug("Received ENQ request");
                        com.bitdreamit.astm.asyncastm.service.states.d.a(d.this, true);
                     } else {
                        com.bitdreamit.astm.asyncastm.service.states.d.d.warn("Error, ENQ expected. Received: " + com.bitdreamit.astm.asyncastm.service.connection.a.a(var1));
                     }
                     continue;
                  } catch (EOFException var6) {
                     com.bitdreamit.astm.asyncastm.service.states.d.d.warn("End of stream reached, it means the connection was closed");
                     d.this.a(g.class);
                  }
               }

               com.bitdreamit.astm.asyncastm.service.states.d.d.debug("Stopped waiting for incoming messages");
               return;
            }
         } catch (InterruptedException var7) {
            com.bitdreamit.astm.asyncastm.service.states.d.d.trace("Interrupted waiting for incoming ASTM messages");
            Thread.currentThread().interrupt();
            return;
         } catch (Exception var8) {
            com.bitdreamit.astm.asyncastm.service.states.d.d.error("Unhandled exception waiting for incoming ASTM messages", var8);
         } finally {
            d.this.b.interrupt();
         }

      }
   };
   Thread a;
   private Runnable k = new Runnable() {
      public final void run() {
         d.this.i.release();

         try {
            d.this.h.acquire();
            d.this.f.a();
            if (d.this.c.e() == null) {
               com.bitdreamit.astm.asyncastm.service.states.d.d.debug("Waiting for new outgoing messages");
               d.this.c.a();
               com.bitdreamit.astm.asyncastm.service.states.d.d.debug("Outgoing message received for sending");
            } else {
               com.bitdreamit.astm.asyncastm.service.states.d.d.debug("Using existing outgoing message for sending");
            }

            com.bitdreamit.astm.asyncastm.service.states.d.d.debug("Stopped checking outgoing messages");
            return;
         } catch (InterruptedException var5) {
            com.bitdreamit.astm.asyncastm.service.states.d.d.trace("Interrupted while checking outgoing messages");
            Thread.currentThread().interrupt();
            return;
         } catch (Exception var6) {
            com.bitdreamit.astm.asyncastm.service.states.d.d.error("Unhandled exception while checking for incoming ASTM messages", var6);
         } finally {
            d.this.a.interrupt();
         }

      }
   };
   Thread b;

   protected final void c() throws IOException {
      super.c();
      this.f = new com.bitdreamit.astm.asyncastm.service.states.file.a();
      this.f.a(0);
      this.e = false;
      this.g = 0;
      this.h = new Semaphore(0);
      this.i = new Semaphore(0);
   }

   public d(com.bitdreamit.astm.asyncastm.service.states.bundle.a var1) throws IOException {
      super(var1);
   }

   public final String a() {
      return "Idle";
   }

   public final AstmConnectionStatus b() {
      return AstmConnectionStatus.IDLE;
   }

   public final void d() throws IOException, InterruptedException {
      this.c.d().c(0);
      this.a = new Thread(this.j, Thread.currentThread().getName() + " -> wait incoming");
      this.b = new Thread(this.k, Thread.currentThread().getName() + " -> wait outgoing");
      this.a.start();
      this.b.start();

      try {
         this.a.join();
         this.b.join();
         if (this.g() instanceof d) {
            if (this.e) {
               if (this.c.b()) {
                  this.c.d().b(21);
                  this.e = false;
                  d.warn("Rejected an incoming ASTM send request because we are still waiting for a previous message to be consumed");
               } else {
                  d.debug("Trying to receive message");
                  this.c.d().b(6);
                  this.a(l.class);
               }
            } else if (this.f.b() && this.c.e() != null) {
               d var8 = this;
               d.debug("Trying to send message");
               this.c.d().c(15);
               this.c.d().b(5);
               if (this.e) {
                  this.i();
               } else {
                  try {
                     int var2;
                     switch(var2 = var8.c.d().a(15)) {
                     case 5:
                        var8.i();
                        break;
                     case 6:
                        var8.a(m.class);
                        break;
                     case 21:
                        var8.f.a(10);
                        ++var8.g;
                        break;
                     default:
                        d.warn("Unrecognized response to enquiry request (" + com.bitdreamit.astm.asyncastm.service.connection.a.a(var2) + ") ");
                     }
                  } catch (TimeoutException var4) {
                     d.warn("Error, sent ENQ but no response after 15 second(s)");
                     this.c.d().b(4);
                     ++this.g;
                  }
               }
            }

            if (this.g >= 6) {
               String var9 = "Maximum number of rejection (6) exceeded in remote machine, message could not be sent.";
               d.error(var9);
               TransmissionResult var10 = new TransmissionResult(TransmissionResult.Status.TIMEOUT, var9);
               this.g = 0;
               this.c.f();
               this.c.a(var10);
            }

            return;
         }
      } catch (InterruptedException var5) {
         TransmissionResult var1 = new TransmissionResult(TransmissionResult.Status.INTERRUPTED, "Message sending task interrupted due to termination of program.");
         this.c.a(var1);
         boolean var7 = false;

         while(!var7) {
            d.debug("State interrupted, waiting child threads");

            try {
               this.a.interrupt();
               this.b.interrupt();
               this.a.join();
               this.b.join();
               var7 = true;
            } catch (InterruptedException var3) {
               d.fatal("Interrupted while joining interrupted subthreads", var3);
               Thread.currentThread().interrupt();
            }
         }

         Thread.currentThread().interrupt();
         return;
      } catch (IOException var6) {
         d.warn("Connection lost, trying to reconnect");
         this.a(g.class);
      }

   }

   private void i() {
      this.e = false;
      if (this.c.d().j()) {
         d.warn("Transfer collision detected. waiting 20 secs before sending ENQ again");
         this.f.a(20);
      } else {
         d.warn("Transfer collision detected. waiting 1 secs before sending ENQ again");
         this.f.a(1);
      }
   }

   static void a(d var0, boolean var1) {
      var0.e = true;
   }
}
