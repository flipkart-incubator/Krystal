package com.flipkart.krystal.vajram.protobuf2024e;

import static com.flipkart.krystal.vajram.protobuf2024e.Protobuf2024e.PROTOBUF_2024_E;

import com.flipkart.krystal.serial.SerdeProtocol;
import com.flipkart.krystal.vajram.protobuf.util.SerializableProtoModel;
import com.google.protobuf.Message;

/** A {@link SerializableProtoModel} whose serde protocol is {@link Protobuf2024e}. */
public interface SerializableProto2024eModel<P extends Message> extends SerializableProtoModel<P> {

  @Override
  default SerdeProtocol _serdeProtocol() {
    return PROTOBUF_2024_E;
  }
}
