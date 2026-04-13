package com.flipkart.krystal.lattice.samples.grpc.proto.sampleProtoService;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.ModelRoot.ModelType;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.serial.SerialId;
import com.flipkart.krystal.vajram.protobuf3.Protobuf3;

@ModelRoot(type = ModelType.RESPONSE)
@SupportedModelProtocols({Protobuf3.class, PlainJavaObject.class})
public interface ProtoMessage extends Model {
  @SerialId(1)
  @IfAbsent(FAIL)
  int count();
}
