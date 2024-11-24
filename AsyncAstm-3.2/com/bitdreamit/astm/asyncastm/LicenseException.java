package com.bitdreamit.astm.asyncastm;

public class LicenseException extends Exception {
   private LicenseException.Error a;

   public LicenseException.Error getError() {
      return this.a;
   }

   public LicenseException(LicenseException.Error var1, String var2) {
      super(var2);
      this.a = var1;
   }

   public LicenseException(LicenseException.Error var1) {
      this(var1, var1.toString());
   }

   public static enum Error {
      NO_LICENSE_PROVIDED,
      BAD_SIGNATURE,
      LICENSE_EXPIRED,
      UNAUTHORIZED_DEVICE,
      VERSION_MISMATCH,
      BEFORE_VALID,
      UNAUTHORIZED_OPERATION,
      MAX_CONNECTIONS_EXCEEDED;
   }
}
