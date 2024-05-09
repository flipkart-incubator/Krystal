package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.vajram.facets.FacetValuesAdaptor;
import java.util.LinkedHashMap;
import java.util.Map;

public record UnBatchedFacets<
        BatchedInputs extends FacetValuesAdaptor, CommonFacets extends FacetValuesAdaptor>(
    BatchedInputs batchedInputs, CommonFacets commonFacets) implements FacetValuesAdaptor {

  @Override
  public Facets toFacetValues() {
    Map<String, FacetValue<Object>> imValues = batchedInputs.toFacetValues().values();
    Map<String, FacetValue<Object>> ciValues = commonFacets.toFacetValues().values();
    LinkedHashMap<String, FacetValue<Object>> merged = new LinkedHashMap<>(imValues);
    merged.putAll(ciValues);
    return new Facets(merged);
  }
}
