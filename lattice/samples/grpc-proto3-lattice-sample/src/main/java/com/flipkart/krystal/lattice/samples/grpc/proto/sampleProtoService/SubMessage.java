package com.flipkart.krystal.lattice.samples.grpc.proto.sampleProtoService;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.ASSUME_DEFAULT_VALUE;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.WILL_NEVER_FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.ModelRoot.ModelType;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.serial.SerialId;
import com.flipkart.krystal.vajram.protobuf3.Protobuf3;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

@ModelRoot(type = ModelType.RESPONSE, builderExtendsModelRoot = true)
@SupportedModelProtocols(Protobuf3.class)
public interface SubMessage extends Model {
  @SerialId(1)
  @IfAbsent(FAIL)
  int count();

  @SerialId(2)
  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable ProtoMessage protoMessage();

  @SerialId(3)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<ProtoMessage> protoMessages();
}
