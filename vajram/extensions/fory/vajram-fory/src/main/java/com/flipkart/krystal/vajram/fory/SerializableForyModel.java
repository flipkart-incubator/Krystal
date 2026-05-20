package com.flipkart.krystal.vajram.fory;

import static com.flipkart.krystal.vajram.fory.Fory.FORY;

import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.serial.SerdeProtocol;
import com.flipkart.krystal.serial.SerializableModel;

/**
 * Marker interface for Krystal model classes whose on-wire representation is produced by Apache
 * Fory. Every generated {@code _ImmutFory} class implements this interface, inheriting the default
 * {@link #_serdeProtocol()} that returns {@link Fory#FORY}.
 */
@SupportedModelProtocols(Fory.class)
public interface SerializableForyModel extends SerializableModel {

  @Override
  default SerdeProtocol _serdeProtocol() {
    return FORY;
  }
}
