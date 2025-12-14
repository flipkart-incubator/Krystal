package com.flipkart.krystal.lattice.codegen.spi.di;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A binding whose value is provided by a provider method
 *
 * @param identifierName The name of this binding logic. Should be globally unique.
 * @param boundType The type which is bound
 * @param dependencies Other bindings this provider depends on
 * @param providingLogic The actual provision logic
 * @param scope The scope of the binding (RequestScoped, Singleton etc.)
 */
public record ProviderMethod(
    String identifierName,
    TypeName boundType,
    List<ParameterSpec> dependencies,
    CodeBlock providingLogic,
    List<AnnotationSpec> annotations,
    @Nullable AnnotationSpec scope)
    implements Binding {

  public ProviderMethod(
      String identifierName,
      TypeName boundType,
      List<ParameterSpec> dependencies,
      CodeBlock providingLogic,
      AnnotationSpec scope) {
    this(identifierName, boundType, dependencies, providingLogic, List.of(), scope);
  }
}
