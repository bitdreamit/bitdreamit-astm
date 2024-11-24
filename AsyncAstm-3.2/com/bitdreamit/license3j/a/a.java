package com.bitdreamit.license3j.a;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

public final class a {
   private final KeyPair a;

   private a(KeyPair var1, String var2) {
      this.a = var1;
   }

   public final KeyPair a() {
      return this.a;
   }

   // $FF: synthetic method
   a(KeyPair var1, String var2, byte var3) {
      this(var1, var2);
   }

   public static class key {
      private static String a(String var0) {
         return var0.contains("/") ? var0.substring(0, var0.indexOf("/")) : var0;
      }

      public static com.bitdreamit.license3j.a.a a(byte[] var0, int var1) throws NoSuchAlgorithmException, InvalidKeySpecException {
         String var5 = a(var0);
         X509EncodedKeySpec var2 = new X509EncodedKeySpec(b(var0));
         PublicKey var10000 = KeyFactory.getInstance(a(a(var0))).generatePublic(var2);
         var2 = null;
         PublicKey var4 = var10000;
         return new a(new KeyPair(var4, (PrivateKey) var2), var5, (byte)0);
      }

      private static String a(byte[] var0) {
         for(int var1 = 0; var1 < var0.length; ++var1) {
            if (var0[var1] == 0) {
               return new String(Arrays.copyOf(var0, var1), StandardCharsets.UTF_8);
            }
         }

         throw new IllegalArgumentException("key does not contain cipher specification");
      }

      private static byte[] b(byte[] var0) {
         for(int var1 = 0; var1 < var0.length; ++var1) {
            if (var0[var1] == 0) {
               return Arrays.copyOfRange(var0, var1 + 1, var0.length);
            }
         }

         throw new IllegalArgumentException("key does not contain algorithm specification");
      }
   }
}
