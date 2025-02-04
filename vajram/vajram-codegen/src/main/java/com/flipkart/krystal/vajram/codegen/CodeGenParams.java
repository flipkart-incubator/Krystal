package com.flipkart.krystal.vajram.codegen;

/**
 * @param isBuilder
 * @param isRequest
 * @param wrapsRequest
 * @param subsetRequest {@code true} when the facets class being generated is a subset of the
 *     complete set of facets (For example: BatchFacets and CommonFacets)
 * @param isUnBatched {@code true} when the facets class is of a batchable vajram
 * @param isSubsetBatch {@code true} when the class being generated is the facet container
 *     containing Batch facets.
 * @param isSubsetCommon{@code true} when the class being generated is the facet container *
 *     containing common facets.
 * @param withImpl
 */
@lombok.Builder(toBuilder = true)
record CodeGenParams(
    boolean isBuilder,
    boolean isRequest,
    boolean wrapsRequest,
    boolean isSubsetBatch,
    boolean isSubsetCommon,
    boolean withImpl) {

  @Override
  public boolean wrapsRequest() {
    return !isRequest() && !isFacetsSubset();
  }

  public boolean isFacetsSubset() {
    return isSubsetBatch() || isSubsetCommon();
  }

  /**
   * Returns whether the code being generated is for a class that will directly be accessed by an
   * application devloper
   */
  public boolean isDevAccessible() {
    return isRequest() || isSubsetBatch();
  }

  /**
   * Returns true if the class designed to the local jvm and not for exchanging data in a
   * distributed context
   */
  public boolean isLocal() {
    return isSubsetBatch();
  }
}
