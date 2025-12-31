package com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.logic;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.ASSUME_DEFAULT_VALUE;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.MAY_FAIL_CONDITIONALLY;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models.ByteArray;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models.JsonResponse;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models.JsonResponse_Immut;
import com.flipkart.krystal.lattice.vajram.sdk.InvocableOutsideProcess;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.json.Json;
import jakarta.inject.Inject;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A sample vajram to demonstrate integration between grpc and vajrams. This vajram can be invoked
 * via a GRPC call by remote clients.
 */
@SuppressWarnings({"initialization.field.uninitialized", "optional.parameter"})
@InvocableOutsideGraph
@InvocableOutsideProcess
@SupportedModelProtocols({Json.class, PlainJavaObject.class})
@Vajram
public abstract class RestLatticeSample extends ComputeVajramDef<JsonResponse> {
  static class _Inputs {

    int optionalInput;

    @IfAbsent(FAIL)
    int mandatoryInput;

    @IfAbsent(value = MAY_FAIL_CONDITIONALLY, conditionalFailureInfo = "In some scenarios")
    int conditionallyMandatoryInput;

    @IfAbsent(ASSUME_DEFAULT_VALUE)
    int inputWithDefaultValue;

    long optionalLongInput;

    @IfAbsent(FAIL)
    long mandatoryLongInput;

    ByteArray optionalByteString;

    @IfAbsent(ASSUME_DEFAULT_VALUE)
    List<Integer> repeatedInts;

    @IfAbsent(FAIL)
    ByteArray defaultByteString;
  }

  static class _InternalFacets {
    @Inject
    @IfAbsent(FAIL)
    JsonResponse_Immut.Builder reponseBuilder;
  }

  @Output
  static JsonResponse output(
      @Nullable Integer optionalInput,
      int mandatoryInput,
      @Nullable Integer conditionallyMandatoryInput,
      int inputWithDefaultValue,
      @Nullable Long optionalLongInput,
      Long mandatoryLongInput,
      @Nullable ByteArray optionalByteString,
      ByteArray defaultByteString,
      JsonResponse_Immut.Builder reponseBuilder) {
    return reponseBuilder
        .string(
            """
              Ding Ding Ding
              $$ optionalInput: %s $$
              $$ mandatoryInput: %s $$
              $$ conditionallyMandatoryInput: %s $$
              $$ inputWithDefaultValue: %s $$
              $$ optionalLongInput: %s $$
              $$ mandatoryLongInput: %s $$
              $$ optionalByteString: %s $$
              $$ defaultByteString: %s ---- $$
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
