package com.flipkart.krystal.vajram.fory;

import static com.flipkart.krystal.vajram.fory.Fory.FORY;

import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.serial.SerializableModel;
import java.io.OutputStream;

/**
 * Marker interface for Krystal model classes whose on-wire representation is produced by Apache
 * Fory. Every generated {@code _ImmutFory} class implements this interface, inheriting the default
 * {@link #_serdeProtocol()} that returns {@link Fory#FORY}.
 */
@SupportedModelProtocol(Fory.class)
public interface SerializableForyModel extends SerializableModel {

  @Override
  default Fory _serdeProtocol() {
    return FORY;
  }

  @Override
  default void _serialize(OutputStream outputStream) {
    Fory.foryInstance().serialize(outputStream, this);
  }
}
