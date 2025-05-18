package com.flipkart.krystal.vajram.protobuf3;

import com.flipkart.krystal.serial.SerializableModel;
import com.google.protobuf.Message;

public interface ProtoSerializableModel extends SerializableModel {
  Message _proto();
}
