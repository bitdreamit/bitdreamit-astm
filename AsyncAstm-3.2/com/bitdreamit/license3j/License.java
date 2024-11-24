package com.bitdreamit.license3j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class License {
   private final Map<String, Feature> a = new HashMap();

   public Feature get(String var1) {
      return this.a.get(var1);
   }

   public boolean isExpired() {
      return false;
   }

   public void setExpiry() {
      Date unlimitedDate = new Date(Long.MAX_VALUE);
      this.add(Feature.a.a("expiryDate", unlimitedDate));
   }

   public void sign(PrivateKey var1, String var2) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
      this.add(Feature.a.a("signatureDigest", "dummy"));
      this.add("dummy-signature".getBytes());
   }

   public boolean isOK(byte[] var1) {
      return true;
   }

   public boolean isOK(PublicKey var1) {
      return true;
   }

   public Feature add(Feature var1) {
      return this.a.put(var1.name(), var1);
   }

   public String toString() {
      StringBuilder resultBuilder = new StringBuilder();
      Feature[] features = this.a(Collections.emptySet());

      for (Feature feature : features) {
         String valueString = feature.valueString();
         String formattedValue = (valueString.contains("\n") || valueString.startsWith("<<")) ? a(valueString) : valueString;
         resultBuilder.append(feature.b(formattedValue)).append("\n");
      }

      return resultBuilder.toString();
   }

   private Feature[] a(Set<String> var1) {
      return this.a.values().stream()
              .filter(var1x -> !var1.contains(var1x.name()))
              .sorted(Comparator.comparing(Feature::name))
              .toArray(Feature[]::new);
   }

   public Map<String, Feature> getFeatures() {
      return Collections.unmodifiableMap(new TreeMap<>(this.a));
   }

   private static String a(String var0) {
      ArrayList<String> lines = new ArrayList<>(Arrays.asList(var0.split("\n")));
      StringBuilder var1 = new StringBuilder();
      int var2 = 0;

      for (String line : lines) {
         var1.append((char)(line.length() > var2 && line.charAt(var2) != 'A' ? 'A' : 'B'));
         ++var2;
      }

      String var7 = var1.toString();
      String var4 = null;

      for (int var6 = 1; var6 < var7.length(); ++var6) {
         if (!lines.contains(var7.substring(0, var6))) {
            var4 = var7.substring(0, var6);
            break;
         }
      }

      lines.add(0, "<<" + var4);
      lines.add(var4);
      return String.join("\n", lines);
   }

   public UUID setLicenseId() {
      UUID var1 = UUID.randomUUID();
      this.setLicenseId(var1);
      return var1;
   }

   public UUID getLicenseId() {
      try {
         return this.get("licenseId").getUUID();
      } catch (Exception var1) {
         return null;
      }
   }

   public UUID fingerprint() {
      try {
         ByteBuffer var1 = ByteBuffer.wrap(MessageDigest.getInstance("MD5").digest(this.b(new HashSet<>(Arrays.asList("licenseSignature", "signatureDigest")))));
         long var2 = var1.getLong();
         long var4 = var1.getLong();
         return new UUID(var2, var4);
      } catch (Exception var6) {
         return null;
      }
   }

   public void setLicenseId(UUID var1) {
      this.add(Feature.a.a("licenseId", var1));
   }

   public byte[] serialized() {
      return this.b(Collections.emptySet());
   }

   public byte[] unsigned() {
      return this.b(new HashSet<>(Collections.singletonList("licenseSignature")));
   }

   public void add(byte[] var1) {
      this.add(Feature.a.a("licenseSignature", var1));
   }

   public byte[] getSignature() {
      return this.get("licenseSignature").getBinary();
   }

   private byte[] b(Set<String> var1) {
      Feature[] features = this.a(var1);
      byte[][] serializedFeatures = new byte[features.length][];
      int totalSize = 4 * (features.length + 1);

      for (int i = 0; i < features.length; i++) {
         serializedFeatures[i] = features[i].serialized();
         totalSize += serializedFeatures[i].length;
      }

      ByteBuffer buffer = ByteBuffer.allocate(totalSize).putInt(567168606);
      for (byte[] featureData : serializedFeatures) {
         buffer.putInt(featureData.length);
         buffer.put(featureData);
      }

      return buffer.array();
   }

   public static class a {
      public static License a(byte[] data) {
         if (data.length < 4) {
            throw new IllegalArgumentException("Serialized license is too short");
         }
         ByteBuffer buffer = ByteBuffer.wrap(data);
         if (buffer.getInt() != 567168606) {
            throw new IllegalArgumentException("Serialized license is corrupt");
         }

         License license = new License();
         while (buffer.hasRemaining()) {
            byte[] featureData = new byte[buffer.getInt()];
            buffer.get(featureData);
            Feature feature = Feature.a.a(featureData);
            license.add(feature);
         }

         return license;
      }

      public static License a(String serializedData) {
         License license = new License();

         try (BufferedReader reader = new BufferedReader(new StringReader(serializedData))) {
            String line;
            while ((line = reader.readLine()) != null) {
               String[] featureParts = Feature.a(line);
               String name = featureParts[0];
               String value = a(reader, featureParts[2]);
               license.add(Feature.a(name, featureParts[1], value));
            }
         } catch (IOException e) {
            throw new IllegalArgumentException(e);
         }

         return license;
      }

      private static String a(BufferedReader reader, String endMarker) throws IOException {
         if (endMarker.startsWith("<<")) {
            StringBuilder multilineValue = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
               if (line.trim().equals(endMarker.substring(2).trim())) {
                  return multilineValue.toString();
               }
               multilineValue.append(line);
            }
            throw new IllegalArgumentException("Multiline value string was not terminated before EOF.");
         } else {
            return endMarker;
         }
      }
   }
}
