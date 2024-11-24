package com.bitdreamit.license3j.io;

/**
 * Enum representing different data input/output formats.
 */
public enum IOFormat {
   /**
    * Binary format for raw data.
    */
   BINARY,

   /**
    * Base64-encoded format for textual representation of binary data.
    */
   BASE64,

   /**
    * String format for textual data.
    */
   STRING;

   /**
    * Returns all enum constants as an array.
    * This is redundant since `values()` already provides this functionality,
    * but kept for compatibility or specific use cases.
    *
    * @return an array of IOFormat constants.
    */
   public static IOFormat[] getFormats() {
      return values(); // Leverage the built-in values() method
   }
}
