package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.vajram.VajramID;
import javax.lang.model.element.VariableElement;
import lombok.Builder;

@Builder
public record DependencyModel(
    String name,
    VajramID depVajramId,
    String depReqClassName,
    boolean isMandatory,
    boolean canFanout,
    String documentation,
    VariableElement depField)
    implements FacetGenModel {}