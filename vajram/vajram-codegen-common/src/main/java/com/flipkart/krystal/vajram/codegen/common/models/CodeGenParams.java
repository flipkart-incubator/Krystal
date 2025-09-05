package com.flipkart.krystal.vajram.codegen.common.models;

/**
 * Used to specify the type of class being generated
 *
 * @param isRequest {@code true} when the class being generated is a request class
 * @param isModelRoot {@code true} when the interface being generated is the root type for that
 *     vajram model hierarchy. For example, Vajram_Req and Vajram_Fac.
 * @param isBuilder {@code true} when the class being generated is a builder.
 * @param isBuilderRoot {@code true} when the interface being generated is the root type for that
 *     vajram model builder hierarchy. For example, Vajram_ReqImmut.Builder and
 *     Vajram_ImmutFacet.Builder
 * @param wrapsRequest {@code true} when the class being generated wraps the request class (Ex:
 *     Facets class)
 * @param isSubsetBatch {@code true} when the class being generated is the facet container
 *     containing Batch facets.
 * @param isSubsetCommon {@code true} when the class being generated is the facet container *
 *     containing common facets.
 * @param withImpl {@code true} when the class being generated is an implementation, not an
 *     interface
 */
@lombok.Builder(toBuilder = true)
public record CodeGenParams(
    boolean isRequest,
    boolean isModelRoot,
    boolean isBuilder,
    boolean isBuilderRoot,
    boolean wrapsRequest,
    boolean wrapsFacets,
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
   * application developer
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

  @Override
  public boolean isBuilder() {
    return isBuilder || isBuilderRoot;
  }
}
