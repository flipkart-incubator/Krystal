package com.flipkart.krystal.vajram.facets;

import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.tags.ElementTags;
import com.flipkart.krystal.vajram.das.DataAccessSpec;
import java.util.function.Function;
import lombok.Builder;

@Builder
public record DependencyDef<T>(
    int id,
    String name,
    DataAccessSpec dataAccessSpec,
    boolean isMandatory,
    boolean isBatched,
    boolean canFanout,
    String documentation,
    ElementTags tags,
    FacetValueGetter getter,
    FacetValueSetter setter)
    implements VajramFacetDefinition {}
