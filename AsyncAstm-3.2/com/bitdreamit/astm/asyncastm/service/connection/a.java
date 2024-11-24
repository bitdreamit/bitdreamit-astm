package com.bitdreamit.astm.asyncastm.service.connection;

public final class a {
   private static String[] a = new String[]{"[NUL]", "[SOH]", "[STX]", "[ETX]", "[EOT]", "[ENQ]", "[ACK]", "[BEL]", "[BS]", "[HT]", "[LF]", "[VT]", "[FF]", "[CR]", "[SO]", "[SI]", "[DLE]", "[DC1]", "[DC2]", "[DC3]", "[DC4]", "[NAK]", "[SYN]", "[ETB]", "[CAN]", "[EM]", "[SUB]", "[ESC]", "[FS]", "[GS]", "[RS]", "[US]"};

   public static final String a(int var0) {
      if (var0 < a.length) {
         return a[var0];
      } else {
         return var0 > 126 ? "[" + var0 + "]" : Character.toString((char)var0);
      }
   }

   public static final String a(String var0) {
      StringBuilder var1 = new StringBuilder();
      char[] var4;
      int var3 = (var4 = var0.toCharArray()).length;

      for(int var2 = 0; var2 < var3; ++var2) {
         char var5 = var4[var2];
         var1.append(a(var5));
      }

      return var1.toString();
   }
}
