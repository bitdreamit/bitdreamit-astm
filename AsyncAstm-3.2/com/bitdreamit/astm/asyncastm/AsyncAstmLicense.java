package com.bitdreamit.astm.asyncastm;

import com.bitdreamit.license3j.License;
import java.util.Date;
import java.util.UUID;

public final class AsyncAstmLicense {
   private static final byte[] a = new byte[]{82, 83, 65, 0, 48, -127, -97, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 3, -127, -115, 0, 48, -127, -119, 2, -127, -127, 0, -115, -124, -43, -12, 84, 69, 13, 106, -36, 40, -82, 84, -85, 117, 19, -24, -81, -93, -40, -27, 95, 69, -110, -81, 10, -71, -20, -118, 10, 67, -43, 71, -4, 32, 95, 27, -45, 74, -109, 47, 111, 14, -89, 79, -7, -114, 84, -28, -14, -87, -16, -86, -62, 68, 46, -60, -54, -17, -14, 120, -34, 38, -42, 102, -48, 52, 9, -6, -90, -82, 12, -74, 81, -77, -84, 81, -73, -33, 34, -18, 122, -8, -36, 98, -19, 114, 42, 1, 118, 117, -72, 123, 101, 81, 68, 118, -40, 123, -75, -51, 86, -124, -26, -122, -21, 20, -103, -105, -23, 96, -58, 126, -106, -120, -118, -71, -104, -61, 36, -93, -116, -19, 119, -29, -79, -14, 12, 105, 2, 3, 1, 0, 1};
   private static final String[] b = new String[]{"time.google.com", "time.cloudflare.com", "es.pool.ntp.org"};
   private static License c;
   private static boolean d = false;
   private static int e = 0;
   private static byte[] f = null;
   private static String g = null;

   public AsyncAstmLicense() {
   }

   public static final void setLicense(License var0) throws LicenseException {
      c = var0;
      d = false;
   }

   public static void setExternalInfo(byte[] var0) {
      f = var0;
      d = false;
   }

   public static void setExtensionVersion(String var0) {
      g = var0;
      d = false;
   }

   public static final void setLicenseCheck(License var0, byte[] var1, String var2) throws LicenseException {
      c = var0;
      f = var1;
      g = var2;
      d = false;
   }

   public static final License getLicense() {
      return c;
   }

   public static final void checkLicenseGeneral() throws LicenseException {
   }

   private static final void a(License var0, byte[] var1, String var2) throws LicenseException {
   }

   public static final void checkLicenseStartServer() throws LicenseException {
   }

   public static final void checkLicenseStartClient()  throws LicenseException {
   }

   public static final void checkLicenseSendMessage()  throws LicenseException {
   }

   public static final void checkLicenseReceiveMessage() throws LicenseException {
   }

   private static boolean a(License var0) {
      return true;
   }

   private static boolean b(License var0) {
      return true;
   }

   private static boolean a(String var0, String var1) {
      String[] var4 = var0.split("\\.");
      String[] var5 = var1.split("\\.");
      if (var4.length < var5.length) {
         return false;
      } else {
         boolean var2 = true;

         for(int var3 = 0; var2 && var3 < var5.length && var3 < var4.length; ++var3) {
            var2 = var5[var3].equals("*") || var5[var3].equals(var4[var3]);
         }

         return var2;
      }
   }

   private static Date a() {
      return new Date();
   }

   private static Date b() {
      return new Date();
   }

   public static final UUID getHardwareId(byte[] var0) {
      return UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
   }

   private static final void c(License var0) throws LicenseException {
      if (e != 0 && d) {
         --e;
      } else {
         e = 10;
         d = true;
      }
   }

   static final void a(int var0) throws LicenseException {
   }
}
