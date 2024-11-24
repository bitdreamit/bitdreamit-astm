package com.bitdreamit.license3j.io;

import com.bitdreamit.license3j.License;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public class LicenseReader implements Closeable {
   private final InputStream a;
   private boolean b;

   public LicenseReader(InputStream var1) {
      this.b = false;
      Objects.requireNonNull(var1);
      this.a = var1;
   }

   public LicenseReader(File var1, long var2) throws FileNotFoundException {
      this((InputStream)(new FileInputStream(var1)));
      if (var1.length() > var2) {
         throw new IllegalArgumentException("License file is too long.");
      }
   }

   public LicenseReader(File var1) throws FileNotFoundException {
      this((InputStream)(new FileInputStream(var1)));
   }

   public LicenseReader(String var1, long var2) throws FileNotFoundException {
      this(new File(var1), var2);
   }

   public LicenseReader(String var1) throws FileNotFoundException {
      this(new File(var1));
   }

   public License read() throws IOException {
      return this.read(IOFormat.BINARY);
   }

   public License read(IOFormat var1) throws IOException {
      License var2;
      switch(var1) {
      case BINARY:
         var2 = License.a.a(com.bitdreamit.license3j.io.a.a(this.a));
         break;
      case BASE64:
         var2 = License.a.a(Base64.getDecoder().decode(com.bitdreamit.license3j.io.a.a(this.a)));
         break;
      case STRING:
         var2 = License.a.a(new String(com.bitdreamit.license3j.io.a.a(this.a), StandardCharsets.UTF_8));
         break;
      default:
         throw new IllegalArgumentException(IOFormat.class.getName() + " is incompatible with License3j, and was used with the value " + var1 + " which is unknown");
      }

      this.close();
      return var2;
   }

   public void close() throws IOException {
      if (!this.b) {
         this.b = true;
         if (this.a != null) {
            this.a.close();
         }

      }
   }
}
