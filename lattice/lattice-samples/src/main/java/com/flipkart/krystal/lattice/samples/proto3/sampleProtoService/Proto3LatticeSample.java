package com.flipkart.krystal.lattice.samples.proto3.sampleProtoService;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.ASSUME_DEFAULT_VALUE;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.MAY_FAIL_CONDITIONALLY;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.lattice.core.RemotelyInvocable;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.serial.ReservedSerialIds;
import com.flipkart.krystal.serial.SerialId;
import com.flipkart.krystal.serial.SupportedSerdeProtocols;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.protobuf3.Protobuf3;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.Optional;

@SuppressWarnings({"initialization.field.uninitialized", "optional.parameter"})
@ExternallyInvocable
@RemotelyInvocable
@SupportedSerdeProtocols(Protobuf3.class)
@ReservedSerialIds(8)
@Vajram
abstract class Proto3LatticeSample extends ComputeVajramDef<Proto3LatticeSampleResponse> {
  static class _Inputs {
    @SerialId(1)
    int optionalInput;

    @SerialId(2)
    @IfAbsent(FAIL)
    int mandatoryInput;

    @SerialId(3)
    @IfAbsent(value = MAY_FAIL_CONDITIONALLY, conditionalFailureInfo = "In some scenarios")
    int conditionallyMandatoryInput;

    @SerialId(4)
    @IfAbsent(ASSUME_DEFAULT_VALUE)
    int inputWithDefaultValue;

    @SerialId(5)
    long optionalLongInput;

    @SerialId(6)
    @IfAbsent(FAIL)
    long mandatoryLongInput;

    @SerialId(7)
    ByteString optionalByteString;

    @IfAbsent(ASSUME_DEFAULT_VALUE)
    @SerialId(9)
    List<Integer> repeatedInts;

    @SerialId(10)
    @IfAbsent(ASSUME_DEFAULT_VALUE)
    ByteString defaultByteString;
  }

  @Output
  static Proto3LatticeSampleResponse output(
      Optional<Integer> optionalInput,
      int mandatoryInput,
      Optional<Integer> conditionallyMandatoryInput,
      int inputWithDefaultValue,
      Optional<Long> optionalLongInput,
      Long mandatoryLongInput,
      Optional<ByteString> optionalByteString,
      ByteString defaultByteString) {
    return Proto3LatticeSampleResponse_ImmutPojo._builder()
        .string(
            """
              $$ optionalInput: %s $$
              $$ mandatoryInput: %s $$
              $$ conditionallyMandatoryInput: %s $$
              $$ inputWithDefaultValue: %s $$
              $$ optionalLongInput: %s $$
              $$ mandatoryLongInput: %s $$
              $$ optionalByteString: %s $$
              $$ defaultByteString: %s $$
              """
                .formatted(
                    optionalInput,
                    mandatoryInput,
                    conditionallyMandatoryInput,
                    inputWithDefaultValue,
                    optionalLongInput,
                    mandatoryLongInput,
                    optionalByteString.map(bytes -> bytes.toString(UTF_8)),
                    defaultByteString.toString(UTF_8)))
        .mandatoryInt(1)
        ._build();
  }
}
