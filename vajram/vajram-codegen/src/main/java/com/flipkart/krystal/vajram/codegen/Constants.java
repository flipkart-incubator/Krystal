package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.vajram.utils.Constants.IMMUT_FACETS_CLASS_SUFFIX;

import com.squareup.javapoet.CodeBlock;

public final class Constants {
  public static final String COGENGEN_PHASE_KEY = "krystal.vajram.codegen.phase";
  public static final String DEP_RESP = "depResp";
  public static final String RESOLVABLE_INPUTS = "resolvableInputs";
  public static final String INPUT_SRC = "inputSrc";
  public static final String VAJRAM_LOGIC_INPUT_ARGS = "outputLogicInputArguments";
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
  public static final String GET_SIMPLE_INPUT_RESOLVERS = "getSimpleInputResolvers";
  public static final String FACETS_CLASS_SUFFIX = "_Fac";
  public static final String FACETS_LIST = "_facetValuesList";
  public static final String BATCH_FACETS = "BatchFacets";
  public static final String BATCH_FACETS_SUFFIX = "_BatchItem";
  public static final String COMMON_INPUTS = "CommonFacets";
  public static final String COMMON_FACETS_SUFFIX = "_CommonFac";
  public static final String COMMON_IMMUT_FACETS_CLASS_SUFFIX =
      "Common" + IMMUT_FACETS_CLASS_SUFFIX;
  public static final String FACETVALUES_VAR = "_facetValues";
  public static final String RESOLVER_REQUESTS = "_resolverRequests";
  public static final String RESOLVER_REQUEST = "_resolverRequest";
  public static final String RESOLVER_RESULTS = "_resolverResults";
  public static final String RESOLVER_RESULT = "_resolverResult";
  public static final String INCOMING_FACETS = "_incomingFacets";
  public static final String BATCHES_VAR = "_batchItems";
  public static final String DEP_REQ_PARAM = "dependecyRequest";
  public static final String _FACETS_CLASS = "_Facets";
  public static final String FACETS_FIELDS_VAR = "facetsFields";
  public static final String REQUEST_SUFFIX = "_Req";
  public static final String IMMUT_REQUEST_SUFFIX = "_ImmutReq";
  public static final String IMMUT_REQUEST_POJO_SUFFIX = "_ImmutReqPojo";
  public static final String INPUT_SPECS_CLASS_SUFFIX = "_InputSpecs";
  public static final String FACET_SPECS_CLASS_SUFFIX = "_FacetSpecs";
  public static final String SPEC_CLASS_SUFFIX = "_SpecType";
  public static final String IMPL_SUFFIX = "_Wrpr";

  public static final CodeBlock EMPTY_CODE_BLOCK = CodeBlock.builder().build();
  public static final String QUALIFIED_FACET_SEPERATOR = ":";

  private Constants() {}
}
