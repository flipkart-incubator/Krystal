package com.flipkart.krystal.lattice.samples.proto3.sampleProtoService;

import static com.flipkart.krystal.data.IfAbsent.IfAbsentThen.DEFAULT_TO_EMPTY;
import static com.flipkart.krystal.data.IfAbsent.IfAbsentThen.DEFAULT_TO_ZERO;
import static com.flipkart.krystal.data.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.data.IfAbsent.IfAbsentThen.MAY_FAIL_CONDITIONALLY;

import com.flipkart.krystal.data.IfAbsent;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.serial.SerialId;
import com.flipkart.krystal.vajram.protobuf3.Protobuf3;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

@ModelRoot
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
  @IfAbsent(DEFAULT_TO_EMPTY)
  List<Integer> optionalIntArray();

  @SerialId(7)
  @IfAbsent(FAIL)
  int mandatoryInt();

  @SerialId(8)
  @IfAbsent(DEFAULT_TO_ZERO)
  int defaultInt();
}
