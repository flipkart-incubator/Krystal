package com.flipkart.krystal.lattice.samples.grpc.proto.sampleProtoService;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.ASSUME_DEFAULT_VALUE;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.MAY_FAIL_CONDITIONALLY;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.lattice.vajram.sdk.InvocableOutsideProcess;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.serial.ReservedSerialIds;
import com.flipkart.krystal.serial.SerialId;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.protobuf3.Protobuf3;
import com.google.protobuf.ByteString;
import jakarta.inject.Inject;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A sample vajram to demonstrate integration between grpc and vajrams. This vajram can be invoked
 * via a GRPC call by remote clients.
 */
@SuppressWarnings("initialization.field.uninitialized")
@InvocableOutsideGraph
@InvocableOutsideProcess
@SupportedModelProtocols(Protobuf3.class)
@ReservedSerialIds(8)
@Vajram
public abstract class Proto3LatticeSample extends ComputeVajramDef<Proto3LatticeSampleResponse> {
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

  static class _InternalFacets {
    @Inject
    @IfAbsent(FAIL)
    Proto3LatticeSampleResponse_Immut.Builder responseBuilder;
  }

  @Output
  static Proto3LatticeSampleResponse output(
      @Nullable Integer optionalInput,
      int mandatoryInput,
      @Nullable Integer conditionallyMandatoryInput,
      int inputWithDefaultValue,
      @Nullable Long optionalLongInput,
      Long mandatoryLongInput,
      @Nullable ByteString optionalByteString,
      ByteString defaultByteString,
      Proto3LatticeSampleResponse_Immut.Builder responseBuilder) {
    return responseBuilder
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
                    optionalByteString == null ? null : optionalByteString.toString(UTF_8),
                    defaultByteString.toString(UTF_8)))
        .mandatoryInt(1)
        ._build();
  }
}
