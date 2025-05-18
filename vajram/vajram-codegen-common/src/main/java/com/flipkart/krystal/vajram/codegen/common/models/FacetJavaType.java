package com.flipkart.krystal.vajram.codegen.common.models;

import static com.flipkart.krystal.facets.FacetType.INPUT;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.EMPTY_CODE_BLOCK;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.FACET_VALUES_VAR;

import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.One2OneDepResponse;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.exception.VajramDefinitionException;
import com.flipkart.krystal.vajram.facets.FacetValidation;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract sealed class FacetJavaType {

  protected final VajramCodeGenUtility util;

  public FacetJavaType(VajramCodeGenUtility util) {
    this.util = util;
  }

  public abstract TypeName javaTypeName(FacetGenModel facet);

  public CodeBlock fieldGetterCode(FacetGenModel facet, CodeGenParams codeGenParams) {
    if (codeGenParams.isFacetsSubset()) {
      return CodeBlock.of("return this.$L.$L()", FACET_VALUES_VAR, facet.name());
    }
    final boolean facetInCurrentClass =
        (codeGenParams.isRequest()
            || !facet.facetTypes().contains(INPUT)
            || !codeGenParams.wrapsRequest());
    if (facetInCurrentClass) {
      return CodeBlock.of("return this.$L", facet.name());
    }
    if (codeGenParams.wrapsRequest()) {
      return CodeBlock.of("return this._request.$L()", facet.name());
    }
    throw new UnsupportedOperationException("This should not happen. " + this);
  }

  public CodeBlock fieldInitializer(FacetGenModel facet) {
    if (util.usePlatformDefault(facet)) {
      try {
        return facet.dataType().defaultValueExpr(util.processingEnv());
      } catch (Exception e) {
        throw new VajramDefinitionException(
            "The datatype "
                + facet.dataType()
                + " does not support a platform default value."
                + " To fix this issue, change the ifNotSet strategy of the @IfAbsent annotation"
                + " to a value which does not allow default value.");
      }
    } else {
      return EMPTY_CODE_BLOCK;
    }
  }

  public Class<?>[] typeAnnotations(FacetGenModel facet, CodeGenParams codeGenParams) {
    return new Class[] {};
  }

  public static final class Actual extends FacetJavaType {

    public Actual(VajramCodeGenUtility util) {
      super(util);
    }

    @Override
    public TypeName javaTypeName(FacetGenModel facet) {
      return util.getTypeName(util.getDataType(facet)).typeName();
    }

    @Override
    public Class<?>[] typeAnnotations(FacetGenModel facet, CodeGenParams codeGenParams) {
      if (!(facet instanceof DependencyModel)
          && !javaTypeName(facet).isPrimitive()
          && !codeGenParams.isSubsetBatch()) {
        return new Class[] {Nullable.class};
      }
      return super.typeAnnotations(facet, codeGenParams);
    }

    @Override
    public CodeBlock fieldGetterCode(FacetGenModel facet, CodeGenParams codeGenParams) {
      if (codeGenParams.isSubsetBatch()) {
        IfAbsent ifAbsent = facet.facetField().getAnnotation(IfAbsent.class);
        if (ifAbsent != null && !ifAbsent.value().usePlatformDefault()) {
          return CodeBlock.of(
              """
              return $T.validateMandatoryFacet(this.$L.$L(), $S, $S)
              """,
              FacetValidation.class,
              FACET_VALUES_VAR,
              facet.name(),
              facet.vajramInfo().vajramId().id(),
              facet.name());
        } else {
          return CodeBlock.of("return this.$L.$L()", FACET_VALUES_VAR, facet.name());
        }
      }
      return super.fieldGetterCode(facet, codeGenParams);
    }
  }

  public static final class Boxed extends FacetJavaType {

    public Boxed(VajramCodeGenUtility util) {
      super(util);
    }

    @Override
    public TypeName javaTypeName(FacetGenModel facet) {
      return util.box(util.getTypeName(util.getDataType(facet))).typeName();
    }

    @Override
    public Class<?>[] typeAnnotations(FacetGenModel facet, CodeGenParams codeGenParams) {
      return new Class<?>[] {Nullable.class};
    }
  }

  public static final class OptionalType extends FacetJavaType {

    public OptionalType(VajramCodeGenUtility util) {
      super(util);
    }

    @Override
    public TypeName javaTypeName(FacetGenModel facet) {
      return util.optional(util.box(util.getTypeName(util.getDataType(facet))));
    }

    @Override
    public CodeBlock fieldInitializer(FacetGenModel facet) {
      return CodeBlock.of("$T.empty()", Optional.class);
    }

    @Override
    public CodeBlock fieldGetterCode(FacetGenModel facet, CodeGenParams codeGenParams) {
      if (codeGenParams.isSubsetBatch()) {
        return CodeBlock.of(
            "return $T.ofNullable(this.$L.$L())", Optional.class, FACET_VALUES_VAR, facet.name());
      }
      return super.fieldGetterCode(facet, codeGenParams);
    }
  }

  public static final class One2OneResponse extends FacetJavaType {

    public One2OneResponse(VajramCodeGenUtility util) {
      super(util);
    }

    @Override
    public TypeName javaTypeName(FacetGenModel facet) {
      return util.responseType((DependencyModel) facet);
    }

    @Override
    public CodeBlock fieldInitializer(FacetGenModel facet) {
      return CodeBlock.of("$T.noRequest()", One2OneDepResponse.class);
    }
  }

  public static final class FanoutResponses extends FacetJavaType {

    public FanoutResponses(VajramCodeGenUtility util) {
      super(util);
    }

    @Override
    public TypeName javaTypeName(FacetGenModel facet) {
      return util.responsesType((DependencyModel) facet);
    }

    @Override
    public CodeBlock fieldInitializer(FacetGenModel facet) {
      return CodeBlock.of("$T.empty()", FanoutDepResponses.class);
    }
  }
}
