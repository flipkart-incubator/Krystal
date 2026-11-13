package com.flipkart.krystal.lattice.codegen.spi;

import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface LatticeAppImplContributor {
  default @Nullable MethodSpec mainMethod(LatticeCodegenContext context) {
    return null;
  }

  default List<MethodSpec> methods(LatticeCodegenContext context) {
    return List.of();
  }

  default List<FieldSpec> fields(LatticeCodegenContext context) {
    return List.of();
  }

  default List<AnnotationSpec> classAnnotations(LatticeCodegenContext context) {
    return List.of();
  }
}
