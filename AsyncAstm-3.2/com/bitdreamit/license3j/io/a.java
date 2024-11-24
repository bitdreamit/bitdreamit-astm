package com.bitdreamit.license3j.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

final class a {
   static byte[] a(InputStream var0) throws IOException {
      ByteArrayOutputStream var1 = new ByteArrayOutputStream();
      byte[] var3 = new byte[4096];

      int var2;
      while((var2 = var0.read(var3)) != -1) {
         var1.write(var3, 0, var2);
      }

      return var1.toByteArray();
   }
}
