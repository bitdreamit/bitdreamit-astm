package com.bitdreamit.astm.asyncastm.service.connection.file;

public final class a extends c {
   public a(String var1) {
      super(var1);
   }

   public final String a() {
      int var1;
      char var2;
      if ((var1 = this.a.length() - this.b) > 240) {
         var1 = 240;
         var2 = 23;
      } else {
         var2 = 3;
      }

      String var3 = this.a.substring(this.b, this.b + var1);
      this.b += var1;
      return var3 + var2;
   }

}
