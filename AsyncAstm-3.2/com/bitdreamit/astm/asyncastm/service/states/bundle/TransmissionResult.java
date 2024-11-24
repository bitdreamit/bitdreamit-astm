package com.bitdreamit.astm.asyncastm.service.states.bundle;

public class TransmissionResult {
   private TransmissionResult.Status a;
   private String b;

   public TransmissionResult(TransmissionResult.Status var1, String var2) {
      this.a = var1;
      this.b = var2;
   }

   public TransmissionResult.Status getStatus() {
      return this.a;
   }

   public void setStatus(TransmissionResult.Status var1) {
      this.a = var1;
   }

   public void setDescription(String var1) {
      this.b = var1;
   }

   public String getDescription() {
      return this.b;
   }

   public static enum Status {
      SUCCESS,
      TIMEOUT,
      DISCONNECTED,
      REJECTED,
      INTERRUPTED,
      UNKNOWN;
   }
}
