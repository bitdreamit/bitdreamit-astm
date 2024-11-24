package com.bitdreamit.astm.asyncastm.service.connection.file;

public final class b extends c {
   public b(String var1) {
      super(var1);
   }

   public final String a() {
      int var2;
      for(var2 = 1; this.b + var2 < this.a.length() && var2 < 240 && this.a.charAt(this.b + var2 - 1) != '\r'; ++var2) {
      }

      char var3;
      if (this.b + var2 != this.a.length() && this.a.charAt(this.b + var2 - 1) != '\r') {
         var3 = 23;
      } else {
         var3 = 3;
      }

      String var1 = this.a.substring(this.b, this.b + var2);
      this.b += var2;
      return var1 + var3;
   }
}
