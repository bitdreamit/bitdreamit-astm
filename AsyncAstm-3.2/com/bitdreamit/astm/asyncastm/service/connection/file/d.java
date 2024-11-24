package com.bitdreamit.astm.asyncastm.service.connection.file;

import com.bitdreamit.astm.asyncastm.service.connection.Protocol;
import com.bitdreamit.license3j.Feature;
import org.apache.log4j.Logger;

public class d {
   private static final Logger a = Logger.getLogger(d.class.getName());
   private StringBuilder b = new StringBuilder();
   private int c = 0;
   private boolean d = false;

   public d(Protocol var1) {
   }

   public final boolean a(String var1) throws IllegalArgumentException {
      StringBuilder var10000 = this.b;
      String var2 = var1;
      String var3 = Feature.a.a(var1);
      StringBuilder var4 = new StringBuilder();
      char var6;
      int var5 = (var6 = var1.charAt(0)) >= '0' && var6 <= '7' ? Character.getNumericValue(var6) : -1;
      int var8;
      if ((var8 = this.c + 1) == 8) {
         var8 = 0;
      }

      if (var5 != var8 && var5 != this.c) {
         a("[0-7]", com.bitdreamit.astm.asyncastm.service.connection.a.a(var1.charAt(0)));
      }

      this.c = var5;
      var5 = 1;
      boolean var9 = false;

      for(var6 = '?'; var5 < var2.length() && !var9; ++var5) {
         if (var5 == 64002) {
            a.warn("Limit of 64000 characters exceeded in message. This is not allowed in ASTM protocol.");
         }

         if ((var6 = var2.charAt(var5)) != 23 && var6 != 3) {
            if (var6 == '\r') {
               var6 = '\n';
            }

            var4.append(var6);
         } else {
            if (!(var1 = var2.substring(var5 + 1, var5 + 3)).equals(var3)) {
               a("Checksum ".concat(String.valueOf(var3)), "Checksum ".concat(String.valueOf(var1)));
            }

            var9 = true;
         }
      }

      if (!var9) {
         a("Valid termination", "Invalid termination (" + var6 + ")");
      }

      var10000.append(var4.toString());
      return false;
   }

   public final String a() {
      return this.b.length() > 0 ? this.b.substring(0, this.b.length()) : "";
   }

   private static void a(String var0, String var1) {
      var0 = "Invalid frame structure. Expected: " + var0 + " Found: " + var1;
      a.error(var0);
      throw new IllegalArgumentException(var0);
   }
}
