package com.flipkart.krystal.vajram.codegen.common.models;

import static com.flipkart.krystal.facets.FacetType.INPUT;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.EMPTY_CODE_BLOCK;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.FACET_SPEC_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.FACET_VALUES_VAR;
import static com.flipkart.krystal.vajram.codegen.common.models.Utils.getFacetsInterfaceName;

import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.One2OneDepResponse;
import com.flipkart.krystal.vajram.exception.VajramDefinitionException;
import com.flipkart.krystal.vajram.facets.FacetValidation;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract sealed class FacetJavaType {

  protected final Utils util;

  public FacetJavaType(Utils util) {
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
    return CodeBlock.of(
        "$T.$L.getPlatformDefaultValue()",
        ClassName.get(
            facet.vajramInfo().packageName(),
            getFacetsInterfaceName(facet.vajramInfo().vajramId().vajramId())),
        facet.name() + FACET_SPEC_SUFFIX);
  }

  public Class<?>[] typeAnnotations(FacetGenModel facet, CodeGenParams codeGenParams) {
    return new Class[] {};
  }

  public static final class Actual extends FacetJavaType {

    public Actual(Utils utils) {
      super(utils);
    }

    @Override
    public TypeName javaTypeName(FacetGenModel facet) {
      return util.getTypeName(util.getDataType(facet)).typeName();
    }

    @Override
    public CodeBlock fieldInitializer(FacetGenModel facet) {
      Mandatory mandatory = facet.facetField().getAnnotation(Mandatory.class);
      if (mandatory != null && mandatory.ifNotSet().usePlatformDefault()) {
        if (facet.dataType().hasPlatformDefaultValue(util.processingEnv())) {
          return CodeBlock.of(
              "$T.$L.getPlatformDefaultValue()",
              ClassName.get(
                  facet.vajramInfo().packageName(),
                  getFacetsInterfaceName(facet.vajramInfo().vajramId().vajramId())),
              facet.name() + FACET_SPEC_SUFFIX);
        } else {
          throw new VajramDefinitionException(
              "The datatype "
                  + facet.dataType()
                  + " does not support a platform default value."
                  + " To fix this issue, change the ifNotSet strategy of the @Mandatory annotation"
                  + " to a value which does not allow default value.");
        }
      } else {
        return EMPTY_CODE_BLOCK;
      }
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
        Mandatory mandatory = facet.facetField().getAnnotation(Mandatory.class);
        if (mandatory != null && !mandatory.ifNotSet().usePlatformDefault()) {
          return CodeBlock.of(
              """
              return $T.validateMandatoryFacet(this.$L.$L(), $S, $S)
              """,
              FacetValidation.class,
              FACET_VALUES_VAR,
              facet.name(),
              facet.vajramInfo().vajramId().vajramId(),
              facet.name());
        } else {
          return CodeBlock.of("return this.$L.$L()", FACET_VALUES_VAR, facet.name());
        }
      }
      return super.fieldGetterCode(facet, codeGenParams);
    }
  }

  public static final class Boxed extends FacetJavaType {

    public Boxed(Utils utils) {
      super(utils);
    }

    @Override
    public TypeName javaTypeName(FacetGenModel facet) {
      return util.box(util.getTypeName(util.getDataType(facet))).typeName();
    }

    @Override
    public Class<?>[] typeAnnotations(FacetGenModel facet, CodeGenParams codeGenParams) {
      return new Class<?>[] {Nullable.class};
    }

    @Override
    public CodeBlock fieldInitializer(FacetGenModel facet) {
      return CodeBlock.of("null");
    }
  }

  public static final class OptionalType extends FacetJavaType {

    public OptionalType(Utils utils) {
      super(utils);
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

    public One2OneResponse(Utils util) {
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

    public FanoutResponses(Utils util) {
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
