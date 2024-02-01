package com.flipkart.krystal.vajram.facets;

import com.flipkart.krystal.vajram.das.DataAccessSpec;
import lombok.Builder;

@Builder
public record DependencyDef<T>(
    String name,
    DataAccessSpec dataAccessSpec,
    boolean isMandatory,
    boolean canFanout,
    String documentation)
    implements VajramFacetDefinition {}
