package com.flipkart.krystal.vajram.modulation;

import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.vajram.facets.FacetValuesAdaptor;
import java.util.LinkedHashMap;
import java.util.Map;

public record UnmodulatedFacets<
        ModulatedInputs extends FacetValuesAdaptor, CommonFacets extends FacetValuesAdaptor>(
    ModulatedInputs modulatedInputs, CommonFacets commonFacets) implements FacetValuesAdaptor {

  @Override
  public Facets toFacetValues() {
    Map<String, FacetValue<Object>> imValues = modulatedInputs.toFacetValues().values();
    Map<String, FacetValue<Object>> ciValues = commonFacets.toFacetValues().values();
    LinkedHashMap<String, FacetValue<Object>> merged = new LinkedHashMap<>(imValues);
    merged.putAll(ciValues);
    return new Facets(merged);
  }
}
