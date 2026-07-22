package com.flipkart.krystal.vajram.ext.sql.data;

import com.flipkart.krystal.model.array.FloatArray;
import com.flipkart.krystal.model.array.SimpleFloatArray;
import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.PolyNull;

@UtilityClass
public class DataUtils {

  public static @PolyNull FloatArray sqlValToFloatArray(@PolyNull Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof float[] floatArray) {
      return SimpleFloatArray.backedBy(floatArray);
    }
    if (value instanceof Float[] floatArray) {
      return SimpleFloatArray.copyOfBoxed(floatArray);
    }
    if (value instanceof FloatArray floatArray) {
      return floatArray;
    }
    throw new IllegalArgumentException("Cannot convert " + value.getClass() + " to FloatArray");
  }

  public static float[] floatArrayToSqlVal(@PolyNull FloatArray value) {
    if (value == null) {
      return null;
    }
    return value.toArray();
  }
}
