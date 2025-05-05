package com.flipkart.krystal.lattice.samples.proto3.sampleProtoService;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.ASSUME_DEFAULT_VALUE;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.MAY_FAIL_CONDITIONALLY;
import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.serial.SerialId;
import com.flipkart.krystal.vajram.protobuf3.Protobuf3;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

@ModelRoot(type = RESPONSE)
@SupportedModelProtocols({PlainJavaObject.class, Protobuf3.class})
public interface Proto3LatticeSampleResponse extends Model {
  @SerialId(1)
  @IfAbsent(FAIL)
  String string();

  @SerialId(2)
  @IfAbsent(MAY_FAIL_CONDITIONALLY)
  Optional<Integer> optionalInteger();

  @SerialId(3)
  @IfAbsent(MAY_FAIL_CONDITIONALLY)
  @Nullable Integer nullableIntegerMayFailConditionally();

  @SerialId(4)
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
}
