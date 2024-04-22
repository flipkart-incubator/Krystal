package com.flipkart.krystal.vajram.facets;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.vajram.das.DataAccessSpec;
import com.google.common.collect.ImmutableMap;
import lombok.Builder;

@Builder
public record DependencyDef<T>(
    int id,
    String name,
    DataAccessSpec dataAccessSpec,
    boolean isMandatory,
    boolean canFanout,
    String documentation,
    ImmutableMap<Object, Tag> tags)
    implements VajramFacetDefinition {}
