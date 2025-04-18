package com.flipkart.krystal.lattice.samples.proto3.sampleProtoService;

import static com.flipkart.krystal.data.IfNull.IfNullThen.DEFAULT_TO_EMPTY;
import static com.flipkart.krystal.data.IfNull.IfNullThen.DEFAULT_TO_ZERO;
import static com.flipkart.krystal.data.IfNull.IfNullThen.FAIL;
import static com.flipkart.krystal.data.IfNull.IfNullThen.MAY_FAIL_CONDITIONALLY;

import com.flipkart.krystal.data.IfNull;
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
  @IfNull(FAIL)
  String string();

  @SerialId(2)
  @IfNull(MAY_FAIL_CONDITIONALLY)
  Optional<Integer> optionalInteger();

  @SerialId(3)
  @IfNull(MAY_FAIL_CONDITIONALLY)
  @Nullable Integer nullableIntegerMayFailConditionally();

  @SerialId(4)
  @Nullable Integer nullableInteger();

  @SerialId(6)
  @IfNull(DEFAULT_TO_EMPTY)
  List<Integer> optionalIntArray();

  @SerialId(7)
  @IfNull(FAIL)
  int mandatoryInt();

  @SerialId(8)
  @IfNull(DEFAULT_TO_ZERO)
  int defaultInt();
}
