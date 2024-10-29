package com.flipkart.krystal.vajram.facets;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.vajram.das.DataAccessSpec;
import com.google.common.collect.ImmutableMap;
import java.util.function.Function;
import lombok.Builder;

@Builder
public record DependencyDef<T>(
    int id,
    String name,
    DataAccessSpec dataAccessSpec,
    boolean isMandatory,
    Function<? extends Facets, Object> getter,
    boolean isBatched,
    boolean canFanout,
    String documentation,
    ImmutableMap<Object, Tag> tags)
    implements VajramFacetDefinition {}
