package com.bitdreamit.astm.asyncastm.service.states.file;

import java.time.Instant;
import org.apache.log4j.Logger;

public class a {
   private static final Logger a = Logger.getLogger(a.class.getName());
   private Instant b;
   private boolean c = true;

   public a() {
      this.a(0);
   }

   public final void a(int var1) {
      this.b = Instant.now().plusSeconds((long)var1);
      this.c = false;
   }

   public final void a() throws InterruptedException {
      long var1;
      if ((var1 = this.c()) > 0L) {
         a.debug("Waiting " + var1 + " second(s)");
         Thread.sleep(var1 * 1000L);
      }

      this.c = true;
   }

   public final boolean b() {
      return this.c || this.c() <= 0L;
   }

   private long c() {
      return this.b.getEpochSecond() - Instant.now().getEpochSecond();
   }
}
