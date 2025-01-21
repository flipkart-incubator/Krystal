package com.flipkart.krystal.vajram.codegen;

public final class Constants {

  public static final String COGENGEN_PHASE_KEY = "krystal.vajram.codegen.phase";

  public static final String VAL_ERR = "valErr";
  public static final String DEP_RESP = "depResp";
  public static final String RESOLVABLE_INPUTS = "resolvableInputs";
  public static final String INPUT_SRC = "inputSrc";
  public static final String INPUT_BATCHING = "inputBatching";
  public static final String COMMON_INPUT = "commonInput";
  public static final String RETURN_TYPE = "facetJavaType";
  public static final String VAJRAM_LOGIC_METHOD = "outputLogicMethod";
  public static final String VAJRAM_LOGIC_INPUT_ARGS = "outputLogicInputArguments";
  public static final String HASH_MAP = "hashMap";
  public static final String ARRAY_LIST = "arrayList";
  public static final String COM_FUTURE = "comFuture";
  public static final String LINK_HASH_MAP = "linkHashMap";
  public static final String MAP = "map";
  public static final String LIST = "list";
  public static final String FACET_DEFINITIONS_VAR = "facetDefinitions";
  public static final String FACET_ID_SUFFIX = "_i";
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
  public static final String GET_SIMPLE_INPUT_RESOLVERS = "getSimpleInputResolvers";
  public static final String FACETS_CLASS_SUFFIX = "Facets";
  public static final String IMMUT_FACETS_CLASS_SUFFIX = "ImmutableFacets";
  public static final String FACETS_LIST = "_facetsList";
  public static final String BATCH_FACETS = "BatchFacets";
  public static final String BATCH_IMMUT_FACETS_CLASS_SUFFIX = "Batch" + IMMUT_FACETS_CLASS_SUFFIX;
  public static final String COMMON_INPUTS = "CommonFacets";
  public static final String COMMON_IMMUT_FACETS_CLASS_SUFFIX =
      "Common" + IMMUT_FACETS_CLASS_SUFFIX;
  public static final String FACETS_VAR = "_facets";
  public static final String RESOLVER_REQUESTS = "_resolverRequests";
  public static final String RESOLVER_REQUEST = "_resolverRequest";
  public static final String RESOLVER_RESULTS = "_resolverResults";
  public static final String RESOLVER_RESULT = "_resolverResult";
  public static final String INCOMING_FACETS = "_incomingFacets";
  public static final String BATCHES_VAR = "_batches";
  public static final String DEP_REQ_PARAM = "dependecyRequest";
  public static final String UNMOD_INPUT = "unmodInput";
  public static final String MOD_INPUT = "modInput";
  public static final String IM_MAP = "imMap";
  public static final String IM_LIST = "imList";
  public static final String FUNCTION = "function";
  public static final String OPTIONAL = "optional";
  public static final String _FACETS_CLASS = "_Facets";
  public static final String FACETS_FIELDS_VAR = "facetsFields";
  public static final String REQUEST_SUFFIX = "_Req";
  public static final String IMMUT_REQUEST_SUFFIX = "_ImmutReq";
  public static final String IMMUT_REQUEST_POJO_SUFFIX = "_ImmutReqPojo";
  public static final String IMPL_SUFFIX = "Impl";

  public static final String INPUT_BATCHING_FUTURE_CODE_BLOCK =
      """
          if($facetsList:L.isEmpty()) {
            return $imMap:T.of();
          }
          $map:T<$inputBatching:T, $facets:T> _batches = new $hashMap:T<>();
          $commonInput:T _common = $facetsList:L.get(0)._common();
          for ($facets:T $facetsVar:L : $facetsList:L) {
            $unmodInput:T _castFacets = ($unmodInput:T) $facetsVar:L;
            $inputBatching:T im = _castFacets._batchable();
            _batches.put(im, $facetsVar:L);
          }
          $map:T<$facets:T, $comFuture:T<$facetJavaType:T>> returnValue = new $linkHashMap:T<>();

          var logicExecResults = $outputLogicMethod:L(%s);
          logicExecResults.forEach((im, future) -> returnValue.put(
                $optional:T.ofNullable(mapping.get(im)).orElseThrow(),
                future.<$facetJavaType:T>thenApply($function:T.identity())));
          return $imMap:T.copyOf(returnValue);
      """;
  public static final String BATCHING_EXECUTE_PREPARE_PARAMS =
      """
          if($facetsList:L.isEmpty()) {
            return $imMap:T.of();
          }
          $map:T<$inputBatching:T, $facets:T> _batches = new $hashMap:T<>();
          $commonInput:T _common = (($unmodInput:T)$facetsList:L.get(0))._common();
          for ($facets:T $facetsVar:L : $facetsList:L) {
            $unmodInput:T _castFacets = ($unmodInput:T) $facetsVar:L;
            $inputBatching:T _batch = _castFacets._batchable();
            _batches.put(_batch, $facetsVar:L);
          }
      """;
  public static final String BATCHING_EXECUTE_PREPARE_RESULTS =
      """
          $map:T<$facets:T, $comFuture:T<$facetJavaType:T>> _returnValue = new $linkHashMap:T<>();

          _output.forEach((_batch, _result) -> _returnValue.put(
                $optional:T.ofNullable(_batches.get(_batch)).orElseThrow(),
                _result.<$facetJavaType:T>thenApply($function:T.identity())));
          return $imMap:T.copyOf(_returnValue);
      """;

  private Constants() {}
}
