package com.flipkart.krystal.vajram.protobuf3;

import static com.flipkart.krystal.vajram.protobuf3.Protobuf3.PROTOBUF_3;

import com.flipkart.krystal.serial.SerdeProtocol;
import com.flipkart.krystal.serial.SerializableModel;
import com.google.protobuf.Message;

public interface SerializableProtoModel<P extends Message> extends SerializableModel {
  P _proto();

  @Override
  default SerdeProtocol _serdeProtocol() {
    return PROTOBUF_3;
  }
}
