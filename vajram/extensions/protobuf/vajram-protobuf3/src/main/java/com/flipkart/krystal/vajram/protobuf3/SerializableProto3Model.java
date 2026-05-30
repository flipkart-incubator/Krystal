package com.flipkart.krystal.vajram.protobuf3;

import static com.flipkart.krystal.vajram.protobuf3.Protobuf3.PROTOBUF_3;

import com.flipkart.krystal.vajram.protobuf.util.SerializableProtoModel;
import com.google.protobuf.Message;

/** A {@link SerializableProtoModel} whose serde protocol is {@link Protobuf3}. */
public interface SerializableProto3Model<P extends Message> extends SerializableProtoModel<P> {

  @Override
  default Protobuf3 _serdeProtocol() {
    return PROTOBUF_3;
  }
}
