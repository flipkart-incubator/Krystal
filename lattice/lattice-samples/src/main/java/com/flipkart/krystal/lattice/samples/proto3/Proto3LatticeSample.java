package com.flipkart.krystal.lattice.samples.proto3;

import static com.flipkart.krystal.data.IfNoValue.Strategy.DEFAULT_TO_EMPTY;
import static com.flipkart.krystal.data.IfNoValue.Strategy.DEFAULT_TO_ZERO;
import static com.flipkart.krystal.data.IfNoValue.Strategy.FAIL;
import static com.flipkart.krystal.data.IfNoValue.Strategy.MAY_FAIL_CONDITIONALLY;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.data.IfNoValue;
import com.flipkart.krystal.lattice.annotations.RemotelyInvocable;
import com.flipkart.krystal.lattice.samples.proto3.models.Proto3LatticeSampleResponse;
import com.flipkart.krystal.lattice.samples.proto3.models.Proto3LatticeSampleResponse_ImmutPojo;
import com.flipkart.krystal.serial.ReservedSerialIds;
import com.flipkart.krystal.serial.SerialId;
import com.flipkart.krystal.serial.SupportedSerdeProtocols;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Input;
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
  static class _Facets {
    @SerialId(1)
    @Input
    int optionalInput;

    @SerialId(2)
    @IfNoValue(then = FAIL)
    @Input
    int mandatoryInput;

    @SerialId(3)
    @IfNoValue(then = MAY_FAIL_CONDITIONALLY, conditionalFailureInfo = "In some scenarios")
    @Input
    int conditionallyMandatoryInput;

    @SerialId(4)
    @IfNoValue(then = DEFAULT_TO_ZERO)
    @Input
    int inputWithDefaultValue;

    @SerialId(5)
    @Input
    byte optionalByteInput;

    @SerialId(6)
    @IfNoValue(then = FAIL)
    @Input
    byte mandatoryByteInput;

    @SerialId(7)
    @Input
    ByteString optionalByteString;

    @IfNoValue(then = DEFAULT_TO_EMPTY)
    @SerialId(9)
    @Input
    List<Integer> repeatedInts;
  }

  @Output
  static Proto3LatticeSampleResponse output(
      Optional<Integer> optionalInput,
      int mandatoryInput,
      Optional<Integer> conditionallyMandatoryInput,
      int inputWithDefaultValue,
      Optional<Byte> optionalByteInput,
      byte mandatoryByteInput,
      Optional<ByteString> optionalByteString) {
    return Proto3LatticeSampleResponse_ImmutPojo._builder()
        .string(
            """
              $$ optionalInput: %s $$
              $$ mandatoryInput: %s $$
              $$ conditionallyMandatoryInput: %s $$
              $$ inputWithDefaultValue: %s $$
              $$ optionalByteInput: %s $$
              $$ mandatoryByteInput: %s $$
              $$ optionalByteString: %s $$
              """
                .formatted(
                    optionalInput,
                    mandatoryInput,
                    conditionallyMandatoryInput,
                    inputWithDefaultValue,
                    optionalByteInput,
                    mandatoryByteInput,
                    optionalByteString.map(bytes -> new String(bytes.toByteArray()))))
        ._build();
  }
}
