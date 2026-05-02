package com.flipkart.krystal.lattice.samples.grpc.proto2024e.sampleProtoService;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.ASSUME_DEFAULT_VALUE;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.WILL_NEVER_FAIL;
import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.serial.SerialId;
import com.flipkart.krystal.vajram.protobuf2024e.Protobuf2024e;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Response model for {@link Proto2024eLatticeSample} */
@ModelRoot(type = {RESPONSE})
@SupportedModelProtocols({PlainJavaObject.class, Protobuf2024e.class})
public interface Proto2024eLatticeSampleResponse extends Model {
  @SerialId(1)
  @IfAbsent(FAIL)
  String string();

  @SerialId(2)
  @IfAbsent(WILL_NEVER_FAIL)
  Optional<Integer> optionalInteger();

  @SerialId(3)
  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable Integer nullableIntegerMayFailConditionally();

  @SerialId(4)
  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable Integer nullableInteger();

  @SerialId(6)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<Integer> optionalIntArray();

  @SerialId(7)
  @IfAbsent(FAIL)
  int mandatoryInt();

  @SerialId(8)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  int defaultInt();

  @SerialId(9)
  @IfAbsent(FAIL)
  @Nullable String mandatoryStringPartialConstruction();

  @SerialId(10)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  @Nullable Map<String, String> mapTypedField();

  @SerialId(11)
  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable ProtoMessage protoMessage();

  @SerialId(12)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<ProtoMessage> protoMessages();

  @SerialId(13)
  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable SubMessage subMessage();

  @SerialId(14)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<SubMessage> subMessages();

  @SerialId(15)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  Map<String, SubMessage> namedSubMessages();

  @SerialId(16)
  @IfAbsent(FAIL)
  Status status();

  @SerialId(17)
  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable Status optionalStatus();

  @SerialId(18)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<Status> statuses();

  @SerialId(19)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  Map<String, Status> namedStatuses();
}
