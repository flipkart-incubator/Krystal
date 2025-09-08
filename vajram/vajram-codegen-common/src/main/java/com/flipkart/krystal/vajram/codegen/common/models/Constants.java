package com.flipkart.krystal.vajram.codegen.common.models;

import static com.flipkart.krystal.model.PlainJavaObject.POJO;

public final class Constants {
  public static final String FACET_NAME_SUFFIX = "_n";
  public static final String FACET_SPEC_SUFFIX = "_s";
  public static final String GET_INPUT_RESOLVERS = "getInputResolvers";
  public static final String GET_SIMPLE_INPUT_RESOLVERS = "getSimpleInputResolvers";

  public static final String FACETS_CLASS_SUFFIX = "_Fac";
  public static final String OUTPUT_LOGIC_INPUT_VAR = "_logicInput";
  public static final String FACETS_LIST = "_facetValuesList";
  public static final String BATCH_ITEM_FACETS_SUFFIX = "_BatchItem";
  public static final String BATCH_KEY_FACETS_SUFFIX = "_BatchKey";
  public static final String BATCHES_VAR = "_batchItems";
  public static final String BATCHED_OUTPUT_VAR = "_batchedOutput";
  public static final String BATCH_KEY_NAME = "_batchKey";

  public static final String FACET_VALUES_VAR = "_facetValues";
  public static final String RESOLVER_REQUESTS = "_resolverRequests";
  public static final String RESOLVER_REQUEST = "_resolverRequest";
  public static final String RESOLVER_RESULTS = "_resolverResults";
  public static final String RESOLVER_RESULT = "_resolverResult";
  public static final String INCOMING_FACETS = "_incomingFacets";
  public static final String _INPUTS_CLASS = "_Inputs";
  public static final String _INTERNAL_FACETS_CLASS = "_InternalFacets";

  public static final String IMMUT_SUFFIX = "Immut";
  public static final String REQUEST_SUFFIX = "_Req";
  public static final String IMMUT_REQUEST_SUFFIX = REQUEST_SUFFIX + IMMUT_SUFFIX;
  public static final String IMMUT_REQUEST_POJO_SUFFIX =
      REQUEST_SUFFIX + IMMUT_SUFFIX + POJO.modelClassesSuffix();
  public static final String IMPL_SUFFIX = "_Wrpr";

  public static final String QUALIFIED_FACET_SEPARATOR = ":";
  public static final String VAJRAM_MODELS_GEN_DIR_NAME = "vajramModels";
  public static final String VAJRAM_ID_CONSTANT_NAME = "_VAJRAM_ID";

  private Constants() {}
}
