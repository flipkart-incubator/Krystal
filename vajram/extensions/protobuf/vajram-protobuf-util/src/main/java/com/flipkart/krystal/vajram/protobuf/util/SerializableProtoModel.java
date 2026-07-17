package com.flipkart.krystal.vajram.protobuf.util;

import com.flipkart.krystal.serial.SerializableModel;
import com.google.protobuf.Message;

/**
 * Common base interface for any model whose on-wire representation is a protobuf message,
 * regardless of which protobuf protocol version (proto3, edition 2024, ...) it uses.
 *
 * <p>Concrete protocol-specific sub-interfaces (e.g. {@code SerializableProto3Model}, {@code
 * SerializableProto2024eModel}) supply the {@code _serdeProtocol()} default.
 */
public interface SerializableProtoModel<P extends Message> extends SerializableModel {
  P _proto();
}
