package com.bitdreamit.astm.asyncastm.service.states.bundle;

public class ReceivedMessage {
   private String a;
   private TransmissionResult b;

   public ReceivedMessage(String var1, TransmissionResult var2) {
      this.a = var1;
      this.b = var2;
   }

   public String getMessage() {
      return this.a;
   }

   public TransmissionResult getResult() {
      return this.b;
   }
}
