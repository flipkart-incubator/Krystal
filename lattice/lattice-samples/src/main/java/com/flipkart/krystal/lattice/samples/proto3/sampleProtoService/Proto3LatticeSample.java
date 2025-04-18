package com.flipkart.krystal.lattice.samples.proto3.sampleProtoService;

import static com.flipkart.krystal.data.IfNull.IfNullThen.DEFAULT_TO_EMPTY;
import static com.flipkart.krystal.data.IfNull.IfNullThen.DEFAULT_TO_ZERO;
import static com.flipkart.krystal.data.IfNull.IfNullThen.FAIL;
import static com.flipkart.krystal.data.IfNull.IfNullThen.MAY_FAIL_CONDITIONALLY;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.data.IfNull;
import com.flipkart.krystal.lattice.core.RemotelyInvocable;
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

@ExternallyInvocable
@RemotelyInvocable
@SupportedSerdeProtocols(Protobuf3.class)
@ReservedSerialIds(8)
@Vajram
abstract class Proto3LatticeSample extends ComputeVajramDef<Proto3LatticeSampleResponse> {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    @SerialId(1)
    int optionalInput;

    @SerialId(2)
    @IfNull(FAIL)
    int mandatoryInput;

    @SerialId(3)
    @IfNull(value = MAY_FAIL_CONDITIONALLY, conditionalFailureInfo = "In some scenarios")
    int conditionallyMandatoryInput;

    @SerialId(4)
    @IfNull(DEFAULT_TO_ZERO)
    int inputWithDefaultValue;

    @SerialId(5)
    long optionalLongInput;

    @SerialId(6)
    @IfNull(FAIL)
    long mandatoryLongInput;

    @SerialId(7)
    ByteString optionalByteString;

    @IfNull(DEFAULT_TO_EMPTY)
    @SerialId(9)
    List<Integer> repeatedInts;
  }

  @Output
  static Proto3LatticeSampleResponse output(
      Optional<Integer> optionalInput,
      int mandatoryInput,
      Optional<Integer> conditionallyMandatoryInput,
      int inputWithDefaultValue,
      Optional<Long> optionalLongInput,
      Long mandatoryLongInput,
      Optional<ByteString> optionalByteString) {
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
              """
                .formatted(
                    optionalInput,
                    mandatoryInput,
                    conditionallyMandatoryInput,
                    inputWithDefaultValue,
                    optionalLongInput,
                    mandatoryLongInput,
                    optionalByteString.map(bytes -> new String(bytes.toByteArray()))))
        .mandatoryInt(1)
        ._build();
  }
}
