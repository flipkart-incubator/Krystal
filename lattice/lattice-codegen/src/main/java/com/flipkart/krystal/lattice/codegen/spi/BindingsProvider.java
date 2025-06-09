package com.flipkart.krystal.lattice.codegen.spi;

import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface BindingsProvider {
  ImmutableList<Binding> bindings(LatticeCodegenContext context);

  sealed interface Binding {}

  enum BindingScope {
    NO_SCOPE,
    APP_LOGIC_SCOPE,
    SINGLETON
  }

  record DopantBinding(ClassName dopantType, ClassName dopantImplType) implements Binding {}

  /**
   * A binding whose value is provided by a provider method
   *
   * @param name The name of this binding logic. Should be globally unique.
   * @param boundType The type which is bound
   * @param dependencies Other bindings this provider depends on
   * @param providingLogic The actual provision logic
   * @param scope The scope of the binding (RequestScoped, Singleton etc). Empty if no scope
   */
  record ProviderBinding(
      String name,
      TypeName boundType,
      List<CodeBlock> dependencies,
      CodeBlock providingLogic,
      BindingScope scope)
      implements Binding {}

  record SimpleBinding(ClassName bindFrom, @Nullable CodeBlock named, BindTo bindTo)
      implements Binding {}

  sealed interface BindTo {
    CodeBlock bindToCode();

    record Provider(CodeBlock bindToCode, BindingScope scope) implements BindTo {}
  }
}
