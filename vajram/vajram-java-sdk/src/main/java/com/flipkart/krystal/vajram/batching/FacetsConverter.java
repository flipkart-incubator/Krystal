package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.vajram.facets.FacetValuesAdaptor;
import java.util.function.Function;

@FunctionalInterface
public interface FacetsConverter<
        BatchedInputs extends FacetValuesAdaptor, CommonFacets extends FacetValuesAdaptor>
    extends Function<Facets, UnBatchedFacets<BatchedInputs, CommonFacets>> {}
