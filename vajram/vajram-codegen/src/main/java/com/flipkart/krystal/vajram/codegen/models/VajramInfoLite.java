package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.vajram.VajramID;
import com.google.common.collect.ImmutableBiMap;

public record VajramInfoLite(
    VajramID vajramId,
    DataType<?> responseType,
    ImmutableBiMap<Integer, String> facetIdNameMapping) {}
