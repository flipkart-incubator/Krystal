package com.flipkart.krystal.vajram;

import com.flipkart.krystal.vajram.facets.FacetValuesAdaptor;

/**
 * @param <T> The response type of the vajram corresponding to this request
 */
@FunctionalInterface
public interface VajramRequest<T> extends FacetValuesAdaptor {}
