package com.bitdreamit.license3j.b;

public final class a {
   // $FF: synthetic method
   static long a(String var0, long var1, long var3) {
      long var6 = var1;
      long var11;
      String var15;
      if ((var15 = var0.trim()).startsWith("0x")) {
         var11 = Long.parseLong(var15.substring(2), 16);
      } else {
         var11 = Long.parseLong(var15);
      }

      long var13;
      if (var11 > var3 && var11 < 2L * var3 + 2L) {
         var13 = var11 - 2L * var3 - 2L;
      } else {
         var13 = var11;
      }

      if (var13 <= var3 && var13 >= var6) {
         return var13;
      } else {
         throw new NumberFormatException(var0);
      }
   }

   public static class c {
      public static long a(String var0) {
         return a.a(var0, Long.MIN_VALUE, Long.MAX_VALUE);
      }
   }

   public static class b {
      public static int a(String var0) {
         return (int) a.a(var0, -2147483648L, 2147483647L);
      }
   }

   public static class d {
      public static short a(String var0) {
         return (short)((int) a.a(var0, -32768L, 32767L));
      }
   }

   public static class key {
      public static byte a(String var0) {
         return (byte)((int) a.a(var0, -128L, 127L));
      }
   }
}
