package com.flipkart.krystal.vajram.modulation;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.vajram.facets.FacetValuesAdaptor;
import java.util.function.Function;

@FunctionalInterface
public interface FacetsConverter<
        ModulatedInputs extends FacetValuesAdaptor, CommonFacets extends FacetValuesAdaptor>
    extends Function<Facets, UnmodulatedFacets<ModulatedInputs, CommonFacets>> {}
