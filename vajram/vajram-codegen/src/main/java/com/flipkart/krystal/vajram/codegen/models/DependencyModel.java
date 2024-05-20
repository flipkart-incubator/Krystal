package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.vajram.VajramID;
import javax.lang.model.element.VariableElement;
import lombok.Builder;
import lombok.NonNull;

@Builder
public record DependencyModel(
    @NonNull String name,
    @NonNull VajramID depVajramId,
    @NonNull DataType<?> responseType,
    @NonNull String depReqClassQualifiedName,
    boolean isMandatory,
    boolean canFanout,
    @NonNull String documentation,
    @NonNull VariableElement facetField)
    implements FacetGenModel {}
