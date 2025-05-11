package com.flipkart.krystal.krystex.caching;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.ImmutableFacetValuesContainer;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This class is a special {@link RequestLevelCache} which allows it to be used for stubbing out
 * dependency vajram calls. This allows a single vajram or a sub-graph of vajrams to be unit-tested
 * in isolation instead of testing the whole vajram graph. This can be done in one of two ways:
 *
 * <p>1. Priming the cache using {@link #primeCache(FacetValues, CompletableFuture)} method: This
 * lets testing code explicitly map a given set of facet values of a vajram to an output. This
 * method is useful when the exact facet values which would be passed to a dependency vajram are
 * known.
 *
 * <p>2. Mocking one of the two injection points provided. The injection points are {@link
 * #getFuture(ImmutableFacetValuesContainer)} and {@link #getValue(ImmutableFacetValuesContainer)}
 * which can be used with mocking tools like Mockito. Test code can spy this object and mock the
 * provided injection points to return a mock value instead of invoking a dependency vajram.
 *
 * <p>Example:
 *
 * <pre>
 *   // Errable mockErrableResult = ...
 *   // Result mockResult = ...
 *   // Exception mockException = ...
 *   var cache = Mockito.spy(TestRequestLevelCache.class);
 *   when(cache.getCachedErrable(any(Vajram1_Fac.class))).thenReturn(mockErrableResult);
 *   when(cache.getCachedValue(any(Vajram2_Fac.class))).thenReturn(mockResult);
 *   when(cache.getCachedErrable(any(Vajram3_Fac.class))).thenReturn(Errable.withError(mockException));
 *   try (KrystexVajramExecutor krystexVajramExecutor =
 *         graph.createExecutor(
 *             VajramTestHarness.prepareForTest(vajramExecutorConfig, cache)
 *                 .buildConfig())) {
 *       future = krystexVajramExecutor.execute(vajramRequest);
 *   }
 * </pre>
 */
public final class TestRequestLevelCache extends RequestLevelCache {
  private static final Object NO_VALUE = new Object();

  public TestRequestLevelCache() {}

  public void primeCache(FacetValues facetValues, CompletableFuture<@Nullable Object> data) {
    super.primeCache(facetValues, data);
  }

  @Override
  @Nullable CompletableFuture<@Nullable Object> getCachedValue(CacheKey cacheKey) {
    CompletableFuture<@Nullable Object> futureStub = getFuture(cacheKey.facets());
    if (futureStub.getNow(null) == NO_VALUE) {
      return super.getCachedValue(cacheKey);
    }
    return futureStub;
  }

  /**
   * Mocking injection point for providing a completableFuture. The future can either in an
   * incomplete state or in a completed state - having completed either normally or exceptionally.
   *
   * @param facets The facets for which cache lookup is being done
   * @return a cache hit
   */
  public CompletableFuture<@Nullable Object> getFuture(ImmutableFacetValuesContainer facets) {
    Object value = getValue(facets);
    if (value instanceof CompletableFuture<?>) {
      throw new IllegalArgumentException(
          "getValue cannot return a CompletableFuture. Use 'getFuture' instead.");
    }
    return completedFuture(value);
  }

  /**
   * Similar to {@link #getFuture(ImmutableFacetValuesContainer)} but a value can directly be
   * provided instead of an Errable to make testing code more clear and concise.
   *
   * @implNote Mocking this method has no effect if {@link
   *     #getFuture(ImmutableFacetValuesContainer)} is already mocked for matching parameters
   *     because this method is called via the actual implementation of the getFuture method.
   * @param facets The facets for which cache lookup is being done
   * @return a mocked cache hit or null if no mocking is done.
   */
  @SuppressWarnings("DataFlowIssue") // Null can be returned by a mocking tool
  public @Nullable Object getValue(
      @SuppressWarnings("unused") ImmutableFacetValuesContainer facets) {
    return NO_VALUE;
  }
}
