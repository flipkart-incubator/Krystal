package com.flipkart.krystal.lattice.samples.grpc.proto2024e.sampleProtoService;

import com.flipkart.krystal.model.DefaultValue;
import com.flipkart.krystal.model.EnumModel;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.serial.SerialId;
import com.flipkart.krystal.vajram.protobuf2024e.Protobuf2024e;

@ModelRoot
@SupportedModelProtocol(PlainJavaObject.class)
@SupportedModelProtocol(Protobuf2024e.class)
public enum Status implements EnumModel {
  @DefaultValue
  @SerialId(0)
  UNKNOWN,
  @SerialId(1)
  PENDING,
  @SerialId(2)
  IN_PROGRESS,
  @SerialId(3)
  COMPLETED,
  @SerialId(4)
  FAILED,
}
