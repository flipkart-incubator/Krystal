package com.flipkart.krystal.lattice.samples.grpc.proto2024e.sampleProtoService;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.ModelRoot.ModelType;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.serial.SerialId;
import com.flipkart.krystal.vajram.protobuf2024e.Protobuf2024e;

@ModelRoot(type = {ModelType.RESPONSE})
@SupportedModelProtocols({Protobuf2024e.class, PlainJavaObject.class})
public interface ProtoMessage extends Model {
  @SerialId(1)
  @IfAbsent(FAIL)
  int count();
}
