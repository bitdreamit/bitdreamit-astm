package com.bitdreamit.astm.asyncastm.service.connection.file;

import com.bitdreamit.license3j.Feature;
import java.util.Iterator;
import org.apache.log4j.Logger;

public abstract class c implements Iterator<String> {
   private static final Logger c = Logger.getLogger(c.class.getName());
   String a;
   int b = 0;
   private int d = 1;

   protected c(String var1) {
      if ((var1 = var1.replace("\r\n", "\r").replace('\n', '\r')).length() == 0 || var1.charAt(var1.length() - 1) != '\r') {
         c.debug("CR not found at the end of the message, adding it automatically");
         var1 = var1 + '\r';
      }

      this.a = var1;
   }

   abstract String a();

   public final String b() {
      int var2 = this.d % 8;
      ++this.d;
      String var1 = var2 + this.a();
      return var1 + Feature.a.a(var1);
   }

   public boolean hasNext() {
      return this.b < this.a.length();
   }

   public final String next() {
      int i = this.d % 8;
      this.d++;
      String str = String.valueOf(i) + a();
      return String.valueOf(str) + Feature.a.a(str);
   }

}
