package com.bitdreamit.license3j;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.bitdreamit.license3j.Feature.b.*;

public class Feature {
   private static final String[] date = new String[]{"yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH", "yyyy-MM-dd"};
   private final String f;
   private final Feature.b c;
   private final byte[] d;

   private Feature(String var1, Feature.b var2, byte[] var3) {
      this.f = var1;
      this.c = var2;
      this.d = var3;
   }

   private static SimpleDateFormat d(String var0) {
      SimpleDateFormat var1;
      (var1 = new SimpleDateFormat(var0)).setTimeZone(TimeZone.getTimeZone("UTC"));
      return var1;
   }

   private static Date e(String var0) {
      String[] var1;
      int var2 = (var1 = date).length;
      int var3 = 0;

      while(var3 < var2) {
         String var4 = var1[var3];

         try {
            return d(var4).parse(var0);
         } catch (ParseException var5) {
            ++var3;
         }
      }

      throw new IllegalArgumentException("Cannot parse " + var0);
   }

   static String[] a(String var0) {
      int var1;
      if ((var1 = var0.indexOf("=")) == -1) {
         throw new IllegalArgumentException("The feature's string representation must have a '=' after the type");
      } else {
         int var2;
         int var3 = (var2 = var0.substring(0, var1).indexOf(":")) == -1 ? var1 : var2;
         String var4 = var0.substring(0, var3).trim();
         String var5 = var3 == var1 ? "STRING" : var0.substring(var2 + 1, var1).trim();
         var0 = var0.substring(var1 + 1);
         return new String[]{var4, var5, var0};
      }
   }

   static Feature a(String var0, String var1, String var2) {
      Feature.b var3;
      Object var4 = (var3 = Feature.b.valueOf(var1)).unstringer.apply(var2);
      return (Feature)var3.factory.apply(var0, var4);
   }

   public String name() {
      return this.f;
   }

//   public String toString() {
//      return this.c((String)this.c.stringer.apply(this.c.objecter.apply(this)));
//   }

   final String b(String var1) {
      return this.f + (this.c == Feature.b.STRING ? "" : ":" + this.c.toString()) + "=" + var1;
   }

   public String valueString() {
      return (String)this.c.stringer.apply(this.c.objecter.apply(this));
   }

   public byte[] serialized() {
      byte[] var1 = this.f.getBytes(StandardCharsets.UTF_8);
      int var2 = 4 + var1.length;
      int var3 = this.c.fixedSize == -1 ? 4 + this.d.length : this.c.fixedSize;
      ByteBuffer var4 = ByteBuffer.allocate(var2 + 4 + var3).putInt(this.c.serialized).putInt(var1.length);
      if (this.c.fixedSize == -1) {
         var4.putInt(this.d.length);
      }

      var4.put(var1).put(this.d);
      return var4.array();
   }

   public boolean isBinary() {
      return this.c == Feature.b.BINARY;
   }

   public boolean isString() {
      return this.c == Feature.b.STRING;
   }

   public boolean isByte() {
      return this.c == BYTE;
   }

   public boolean isShort() {
      return this.c == SHORT;
   }

   public boolean isInt() {
      return this.c == INT;
   }

   public boolean isLong() {
      return this.c == Feature.b.LONG;
   }

   public boolean isFloat() {
      return this.c == Feature.b.FLOAT;
   }

   public boolean isDouble() {
      return this.c == Feature.b.DOUBLE;
   }

   public boolean isBigInteger() {
      return this.c == Feature.b.BIGINTEGER;
   }

   public boolean isBigDecimal() {
      return this.c == Feature.b.BIGDECIMAL;
   }

   public boolean isDate() {
      return this.c == Feature.b.DATE;
   }

   public boolean isUUID() {
      return this.c == Feature.b.UUID_CONSTANT;
   }

   public byte[] getBinary() {
      if (this.c != Feature.b.BINARY) {
         throw new IllegalArgumentException("Feature is not BINARY");
      } else {
         return this.d;
      }
   }

   public String getString() {
      if (this.c != Feature.b.STRING) {
         throw new IllegalArgumentException("Feature is not STRING");
      } else {
         return new String(this.d, StandardCharsets.UTF_8);
      }
   }

   public byte getByte() {
      if (this.c != BYTE) {
         throw new IllegalArgumentException("Feature is not BYTE");
      } else {
         return this.d[0];
      }
   }

   public short getShort() {
      if (this.c != SHORT) {
         throw new IllegalArgumentException("Feature is not SHORT");
      } else {
         return ByteBuffer.wrap(this.d).getShort();
      }
   }

   public int getInt() {
      if (this.c != INT) {
         throw new IllegalArgumentException("Feature is not INT");
      } else {
         return ByteBuffer.wrap(this.d).getInt();
      }
   }

   public long getLong() {
      if (this.c != Feature.b.LONG) {
         throw new IllegalArgumentException("Feature is not LONG");
      } else {
         return ByteBuffer.wrap(this.d).getLong();
      }
   }

   public float getFloat() {
      if (this.c != Feature.b.FLOAT) {
         throw new IllegalArgumentException("Feature is not FLOAT");
      } else {
         return ByteBuffer.wrap(this.d).getFloat();
      }
   }

   public double getDouble() {
      if (this.c != Feature.b.DOUBLE) {
         throw new IllegalArgumentException("Feature is not DOUBLE");
      } else {
         return ByteBuffer.wrap(this.d).getDouble();
      }
   }

   public BigInteger getBigInteger() {
      if (this.c != Feature.b.BIGINTEGER) {
         throw new IllegalArgumentException("Feature is not BIGINTEGER");
      } else {
         return new BigInteger(this.d);
      }
   }

   public Date getDate() {
      if (this.c != Feature.b.DATE) {
         throw new IllegalArgumentException("Feature is not DATE");
      } else {
         return new Date(ByteBuffer.wrap(this.d).getLong());
      }
   }

   public BigDecimal getBigDecimal() {
      if (this.c != Feature.b.BIGDECIMAL) {
         throw new IllegalArgumentException("Feature is not BIGDECIMAL");
      } else {
         int var1 = ByteBuffer.wrap(this.d).getInt(this.d.length - 4);
         return new BigDecimal(new BigInteger(Arrays.copyOf(this.d, this.d.length - 4)), var1);
      }
   }

   public UUID getUUID() {
      if (this.c != Feature.b.UUID_CONSTANT) {
         throw new IllegalArgumentException("Feature is not UUID");
      } else {
         ByteBuffer var1;
         long var2 = (var1 = ByteBuffer.wrap(this.d)).getLong();
         long var4 = var1.getLong();
         return new UUID(var4, var2);
      }
   }

   // $FF: synthetic method
   static String date(Object var0) {
      return d(date[0]).format(var0);
   }

   // $FF: synthetic method
   Feature(String var1, Feature.b var2, byte[] var3, byte var4) {
      this(var1, var2, var3);
   }

   static enum b {
      BINARY(1, -1, Feature::getBinary, (var0, var1) -> {
         return Feature.a.a(var0, (byte[])var1);
      }, (var0) -> {
         return Base64.getEncoder().encodeToString((byte[])var0);
      }, (var0) -> {
         return Base64.getDecoder().decode(var0);
      }),
      STRING(2, -1, Feature::getString, (var0, var1) -> {
         return Feature.a.a(var0, (String)var1);
      }, Object::toString, (var0) -> {
         return var0;
      }),
      BYTE(3, 1, Feature::getByte, (var0, var1) -> {
         Byte var2 = (Byte)var1;
         Objects.requireNonNull(var2);
         return createFeatureForByte(var0, var2);
      }, (var0) -> {
         return String.format("0x%02X", (Byte)var0);
      }, com.bitdreamit.license3j.b.a.key::a),
      SHORT(4, 2, Feature::getShort, (var0, var1) -> {
         Short var2 = (Short)var1;
         Objects.requireNonNull(var2);
         return createFeatureForShort(var0, var2);
      }, Object::toString, com.bitdreamit.license3j.b.a.d::a),
      INT(5, 4, Feature::getInt, (var0, var1) -> {
         Integer var2 = (Integer)var1;
         Objects.requireNonNull(var2);
         return createFeatureForInt(var0, var2);
      }, Object::toString, com.bitdreamit.license3j.b.a.b::a),
      LONG(6, 8, Feature::getLong, (var0, var1) -> {
         Long var2 = (Long)var1;
         Objects.requireNonNull(var2);

         return createFeatureForLong(var0, var2);
      }, Object::toString, com.bitdreamit.license3j.b.a.c::a),
      FLOAT(7, 4, Feature::getFloat, (var0, var1) -> {
         Float var2 = (Float)var1;
         Objects.requireNonNull(var2);

         return createFeatureForFloat(var0, var2);

      }, Object::toString, Float::parseFloat),
      DOUBLE(8, 8, Feature::getDouble, (var0, var1) -> {
         Double var2 = (Double)var1;
         Objects.requireNonNull(var2);

         return createFeatureForDouble(var0, var2);

      }, Object::toString, Double::parseDouble),
      BIGINTEGER(9, -1, Feature::getBigInteger, (var0, var1) -> {
         BigInteger var2 = (BigInteger)var1;
         Objects.requireNonNull(var2);

         return createFeatureForBigInteger(var0, var2);
      }, Object::toString, BigInteger::new),
      BIGDECIMAL(10, -1, Feature::getBigDecimal, (var0, var1) -> {
         BigDecimal var3 = (BigDecimal)var1;
         Objects.requireNonNull(var3);
         byte[] var2 = var3.unscaledValue().toByteArray();
         return createFeatureForBigDecimal(var0,var2,var3);
         }, Object::toString, BigDecimal::new),
      DATE(11, 8, Feature::getDate, (var0, var1) -> {
         return Feature.a.a(var0, (Date)var1);
      }, (var0) -> {
         return Feature.date(var0);
      }, (var0) -> {
         return Feature.e(var0);
      }),
      UUID_CONSTANT(12, 16, Feature::getUUID, (var0, var1) -> {
         return Feature.a.a(var0, (UUID)var1);
      }, Object::toString, UUID::fromString);

      final int fixedSize;
      final int serialized;
      final Function<Object, String> stringer;
      final Function<Feature, Object> objecter;
      final Function<String, Object> unstringer;
      final BiFunction<String, Object, Feature> factory;

      private b(int var3, int var4, Function<Feature, Object> var5, BiFunction<String, Object, Feature> var6, Function<Object, String> var7, Function<String, Object> var8) {
         this.serialized = var3;
         this.fixedSize = var4;
         this.stringer = var7;
         this.objecter = var5;
         this.unstringer = var8;
         this.factory = var6;
      }
   }

   private static Feature createFeatureForByte(String var0, Byte var1) {
      return new Feature(var0, BYTE, new byte[]{var1}, (byte)0);
   }
   private static Feature createFeatureForShort(String var0, Short var1) {
      return new Feature(var0, SHORT, ByteBuffer.allocate(2).putShort(var1).array(), (byte)0);
   }

   private static Feature createFeatureForInt(String var0, Integer var1) {
      return new Feature(var0, INT, ByteBuffer.allocate(4).putInt(var1).array(), (byte)0);
   }
   private static Feature createFeatureForLong(String var0, Long var1) {
      return new Feature(var0, LONG, ByteBuffer.allocate(8).putLong(var1).array(), (byte)0);
   }
   private static Feature createFeatureForFloat(String var0, Float var1) {
      return new Feature(var0, FLOAT, ByteBuffer.allocate(4).putFloat(var1).array(), (byte)0);
   }
   private static Feature createFeatureForDouble(String var0, Double var1) {
      return new Feature(var0, DOUBLE, ByteBuffer.allocate(8).putDouble(var1).array(), (byte)0);
   }
   private static Feature createFeatureForBigInteger(String var0, BigInteger var1) {
      return new Feature(var0, BIGINTEGER, var1.toByteArray(), (byte)0);
   }
   private static Feature createFeatureForBigDecimal(String var0, byte[] var1, BigDecimal var2) {
      return new Feature(var0, BIGDECIMAL, ByteBuffer.allocate(4 + var1.length).put(var1).putInt(var2.scale()).array(), (byte)0);
   }

   /*



   * */

   public static class a {
      public static Feature a(String var0, byte[] var1) {
         Objects.requireNonNull(var1);
         return new Feature(var0, Feature.b.BINARY, var1, (byte)0);
      }

      public static Feature a(String var0, String var1) {
         Objects.requireNonNull(var1);
         return new Feature(var0, Feature.b.STRING, var1.getBytes(StandardCharsets.UTF_8), (byte)0);
      }

      public static Feature a(String var0, UUID var1) {
         Objects.requireNonNull(var1);
         return new Feature(var0, Feature.b.UUID_CONSTANT, ByteBuffer.allocate(16).putLong(var1.getLeastSignificantBits()).putLong(var1.getMostSignificantBits()).array(), (byte)0);
      }

      public static Feature a(String var0, Date var1) {
         Objects.requireNonNull(var1);
         return new Feature(var0, Feature.b.DATE, ByteBuffer.allocate(8).putLong(var1.getTime()).array(), (byte)0);
      }

      public static Feature a(byte[] var0) {
         Objects.requireNonNull(var0);
         if (var0.length < 8) {
            throw new IllegalArgumentException("Cannot load feature from a byte array that has " + var0.length + " bytes which is < 8");
         } else {
            ByteBuffer var1;
            Feature.b var2 = a((var1 = ByteBuffer.wrap(var0)).getInt());
            int var3;
            if ((var3 = var1.getInt()) < 0) {
               throw new IllegalArgumentException("Name size is too big. 31bit length should be enough.");
            } else {
               int var4;
               if ((var4 = var2.fixedSize == -1 ? var1.getInt() : var2.fixedSize) < 0) {
                  throw new IllegalArgumentException("Value size is too big. 31bit length should be enough.");
               } else {
                  byte[] var5 = new byte[var3];
                  if (var3 > 0) {
                     if (var1.remaining() < var3) {
                        throw new IllegalArgumentException("Feature binary is too short. It is " + (var4 + var3 - var1.remaining()) + " bytes shy.");
                     }

                     var1.get(var5);
                  }

                  byte[] var7 = new byte[var4];
                  if (var4 > 0) {
                     if (var1.remaining() < var4) {
                        throw new IllegalArgumentException("Feature binary is too short. It is " + (var4 - var1.remaining()) + " bytes shy.");
                     }

                     var1.get(var7);
                  }

                  if (var1.remaining() > 0) {
                     throw new IllegalArgumentException("Cannot load feature from a byte array that has " + var0.length + " bytes which is " + var1.remaining() + " bytes too long");
                  } else {
                     String var6 = new String(var5, StandardCharsets.UTF_8);
                     return new Feature(var6, var2, var7, (byte)0);
                  }
               }
            }
         }
      }

      private static Feature.b a(int var0) {
         Feature.b[] var1;
         int var2 = (var1 = Feature.b.values()).length;

         for(int var3 = 0; var3 < var2; ++var3) {
            Feature.b var4;
            if ((var4 = var1[var3]).serialized == var0) {
               return var4;
            }
         }

         throw new IllegalArgumentException("The deserialized form has a type value " + var0 + " which is not valid.");
      }

      public static String a(String var0) {
         int var1 = 0;
         int var2 = 0;
         if (var0.charAt(0) == 2) {
            var2 = 1;
         }

         do {
            var1 += var0.charAt(var2);
            ++var2;
         } while(var2 < var0.length() && var0.charAt(var2 - 1) != 23 && var0.charAt(var2 - 1) != 3);

         if ((var0 = Integer.toHexString(var1 &= 255).toUpperCase()).length() == 1) {
            var0 = "0".concat(String.valueOf(var0));
         }

         return var0;
      }
   }
}
