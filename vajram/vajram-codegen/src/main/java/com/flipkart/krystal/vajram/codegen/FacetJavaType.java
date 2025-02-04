package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.facets.FacetType.INPUT;
import static com.flipkart.krystal.vajram.codegen.Constants.EMPTY_CODE_BLOCK;
import static com.flipkart.krystal.vajram.codegen.Constants.FACETS_VAR;
import static com.flipkart.krystal.vajram.codegen.Constants.FACET_SPEC_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.Utils.getFacetsInterfaceName;

import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.One2OneDepResponse;
import com.flipkart.krystal.vajram.codegen.models.DependencyModel;
import com.flipkart.krystal.vajram.codegen.models.FacetGenModel;
import com.flipkart.krystal.vajram.exception.VajramDefinitionException;
import com.flipkart.krystal.vajram.facets.FacetValidation;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

abstract sealed class FacetJavaType {

  protected final Utils util;

  public FacetJavaType(Utils util) {
    this.util = util;
  }

  abstract TypeName javaTypeName(FacetGenModel facet);

  CodeBlock fieldGetterCode(FacetGenModel facet, CodeGenParams codeGenParams) {
    if (codeGenParams.isFacetsSubset()) {
      return CodeBlock.of("return this.$L.$L()", FACETS_VAR, facet.name());
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

  CodeBlock fieldInitializer(FacetGenModel facet) {
    return CodeBlock.of(
        "$T.$L.getPlatformDefaultValue()",
        ClassName.get(
            facet.vajramInfo().packageName(),
            getFacetsInterfaceName(facet.vajramInfo().vajramId().vajramId())),
        facet.name() + FACET_SPEC_SUFFIX);
  }

  Class<?>[] typeAnnotations(FacetGenModel facet, CodeGenParams codeGenParams) {
    return new Class[] {};
  }

  static final class Actual extends FacetJavaType {

    public Actual(Utils utils) {
      super(utils);
    }

    @Override
    public TypeName javaTypeName(FacetGenModel facet) {
      return util.getTypeName(util.getDataType(facet)).typeName();
    }

    @Override
    CodeBlock fieldInitializer(FacetGenModel facet) {
      Mandatory mandatory = facet.mandatoryAnno();
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
    Class<?>[] typeAnnotations(FacetGenModel facet, CodeGenParams codeGenParams) {
      if (!(facet instanceof DependencyModel)
          && !javaTypeName(facet).isPrimitive()
          && !codeGenParams.isSubsetBatch()) {
        return new Class[] {Nullable.class};
      }
      return super.typeAnnotations(facet, codeGenParams);
    }

    @Override
    CodeBlock fieldGetterCode(FacetGenModel facet, CodeGenParams codeGenParams) {
      if (codeGenParams.isSubsetBatch()) {
        Mandatory mandatory = facet.mandatoryAnno();
        if (mandatory != null && !mandatory.ifNotSet().usePlatformDefault()) {
          return CodeBlock.of(
              """
              return $T.validateMandatoryFacet(this.$L.$L(), $S, $S)
              """,
              FacetValidation.class,
              FACETS_VAR,
              facet.name(),
              facet.vajramInfo().vajramId().vajramId(),
              facet.name());
        } else {
          return CodeBlock.of("return this.$L.$L()", FACETS_VAR, facet.name());
        }
      }
      return super.fieldGetterCode(facet, codeGenParams);
    }
  }

  static final class Boxed extends FacetJavaType {

    public Boxed(Utils utils) {
      super(utils);
    }

    @Override
    TypeName javaTypeName(FacetGenModel facet) {
      return util.box(util.getTypeName(util.getDataType(facet))).typeName();
    }

    @Override
    Class<?>[] typeAnnotations(FacetGenModel facet, CodeGenParams codeGenParams) {
      return new Class<?>[] {Nullable.class};
    }

    @Override
    CodeBlock fieldInitializer(FacetGenModel facet) {
      return CodeBlock.of("null");
    }
  }

  static final class OptionalType extends FacetJavaType {

    public OptionalType(Utils utils) {
      super(utils);
    }

    @Override
    TypeName javaTypeName(FacetGenModel facet) {
      return util.optional(util.box(util.getTypeName(util.getDataType(facet))));
    }

    @Override
    CodeBlock fieldInitializer(FacetGenModel facet) {
      return CodeBlock.of("$T.empty()", Optional.class);
    }

    @Override
    CodeBlock fieldGetterCode(FacetGenModel facet, CodeGenParams codeGenParams) {
      if (codeGenParams.isSubsetBatch()) {
        return CodeBlock.of(
            "return $T.ofNullable(this.$L.$L())", Optional.class, FACETS_VAR, facet.name());
      }
      return super.fieldGetterCode(facet, codeGenParams);
    }
  }

  static final class One2OneResponse extends FacetJavaType {

    public One2OneResponse(Utils util) {
      super(util);
    }

    @Override
    TypeName javaTypeName(FacetGenModel facet) {
      return util.responseType((DependencyModel) facet);
    }

    @Override
    CodeBlock fieldInitializer(FacetGenModel facet) {
      return CodeBlock.of("$T.noRequest()", One2OneDepResponse.class);
    }
  }

  static final class FanoutResponses extends FacetJavaType {

    public FanoutResponses(Utils util) {
      super(util);
    }

    @Override
    TypeName javaTypeName(FacetGenModel facet) {
      return util.responsesType((DependencyModel) facet);
    }

    @Override
    CodeBlock fieldInitializer(FacetGenModel facet) {
      return CodeBlock.of("$T.empty()", FanoutDepResponses.class);
    }
  }
}
