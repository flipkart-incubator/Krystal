package com.flipkart.krystal.vajram.codegen;

public final class Constants {

  public static final String COGENGEN_PHASE_KEY = "krystal.vajram.codegen.phase";

  public static final String VAL_ERR = "valErr";
  public static final String DEP_RESP = "depResp";
  public static final String RESOLVABLE_INPUTS = "resolvableInputs";
  public static final String INPUT_SRC = "inputSrc";
  public static final String INPUT_BATCHING = "inputBatching";
  public static final String COMMON_INPUT = "commonInput";
  public static final String RETURN_TYPE = "returnType";
  public static final String VAJRAM_LOGIC_METHOD = "outputLogicMethod";
  public static final String VAJRAM_LOGIC_INPUT_ARGS = "outputLogicInputArguments";
  public static final String HASH_MAP = "hashMap";
  public static final String ARRAY_LIST = "arrayList";
  public static final String COM_FUTURE = "comFuture";
  public static final String LINK_HASH_MAP = "linkHashMap";
  public static final String MAP = "map";
  public static final String LIST = "list";
  public static final String FACET_DEFINITIONS_VAR = "facetDefinitions";
  public static final String FACET_NAME_SUFFIX = "_n";
  public static final String FACET_SPEC_SUFFIX = "_s";

  public static final char DOT_SEPARATOR = '.';

  public static final String RESPONSE = "response";
  public static final String VARIABLE = "variable";
  public static final String DEP_RESPONSE = "depResponse";
  public static final String SKIPPED_EXCEPTION = "skippedException";
  public static final String ILLEGAL_ARGUMENT = "illegalArgument";
  public static final String STACKTRACELESS_ARGUMENT = "stacktracelessArgument";
  public static final String REQUEST = "request";
  public static final String RESPONSES_SUFFIX = "Responses";
  public static final String METHOD_GET_FACETS_CONVERTOR = "getBatchFacetsConvertor";
  public static final String METHOD_EXECUTE = "execute";
  public static final String METHOD_RESOLVE_INPUT_OF_DEPENDENCY = "resolveInputOfDependency";
  public static final String METHOD_EXECUTE_COMPUTE = "executeCompute";
  public static final String GET_FACET_DEFINITIONS = "getFacetDefinitions";
  public static final String GET_INPUT_RESOLVERS = "getInputResolvers";
  public static final String FACETS_CLASS_SUFFIX = "Facets";
  public static final String IMMUT_FACETS_CLASS_SUFFIX = "ImmutableFacets";
  public static final String INPUTS_LIST = "facetsList";
  public static final String BATCH_FACETS = "BatchFacets";
  public static final String BATCH_IMMUT_FACETS_CLASS_SUFFIX = "Batch" + IMMUT_FACETS_CLASS_SUFFIX;
  public static final String COMMON_FACETS = "CommonFacets";
  public static final String COMMON_IMMUT_FACETS_CLASS_SUFFIX =
      "Common" + IMMUT_FACETS_CLASS_SUFFIX;
  public static final String FACETS = "facets";
  public static final String DEP_REQ_PARAM = "dependecyRequest";
  public static final String UNMOD_INPUT = "unmodInput";
  public static final String MOD_INPUT = "modInput";
  public static final String IM_MAP = "imMap";
  public static final String IM_LIST = "imList";
  public static final String FUNCTION = "function";
  public static final String OPTIONAL = "optional";
  public static final String _FACETS_CLASS = "_Facets";
  public static final String FACETS_FIELDS_VAR = "facetsFields";
  public static final String REQUEST_SUFFIX = "Request";
  public static final String IMMUT_REQUEST_SUFFIX = "ImmutableRequest";
  public static final String IMPL_SUFFIX = "Impl";

  public static final String INPUT_BATCHING_FUTURE_CODE_BLOCK =
      """
          $map:T<$inputBatching:T, $facets:T> mapping = new $hashMap:T<>();
          $commonInput:T commonFacets = null;
          for ($facets:T facets : facetsList) {
            $unmodInput:T allInputs = ($unmodInput:T) facets;
            commonFacets = allInputs._common();
            $inputBatching:T im = allInputs._batchable();
            mapping.put(im, facets);
          }
          $map:T<$facets:T, $comFuture:T<$returnType:T>> returnValue = new $linkHashMap:T<>();

          if (commonFacets != null) {
            var logicExecResults = $outputLogicMethod:L(%s);
            logicExecResults.forEach((im, future) -> returnValue.put(
                  $optional:T.ofNullable(mapping.get(im)).orElseThrow(),
                  future.<$returnType:T>thenApply($function:T.identity())));
          }
          return $imMap:T.copyOf(returnValue);
      """;

  private Constants() {}
}
