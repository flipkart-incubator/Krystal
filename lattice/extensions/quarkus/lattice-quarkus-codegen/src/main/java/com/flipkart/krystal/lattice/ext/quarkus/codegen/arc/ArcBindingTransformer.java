package com.flipkart.krystal.lattice.ext.quarkus.codegen.arc;

import com.flipkart.krystal.lattice.ext.cdi.codegen.BindingCodeTransformer;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.MethodSpec;
import io.quarkus.arc.Unremovable;

@AutoService(BindingCodeTransformer.class)
public class ArcBindingTransformer implements BindingCodeTransformer {

  @Override
  public void transform(MethodSpec.Builder methodSpecBuilder) {
    methodSpecBuilder.addAnnotation(Unremovable.class);
  }
}
