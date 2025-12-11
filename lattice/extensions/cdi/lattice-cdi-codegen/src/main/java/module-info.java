module flipkart.krystal.lattice.ext.cdi.codegen {
  exports com.flipkart.krystal.lattice.ext.cdi.codegen;

  provides com.flipkart.krystal.lattice.codegen.spi.di.DepInjectBinderGen with
      com.flipkart.krystal.lattice.ext.cdi.codegen.CdiBinderGen;
  provides com.flipkart.krystal.lattice.codegen.spi.di.BindingsProvider with
      com.flipkart.krystal.lattice.ext.cdi.codegen.CIDepInjectionBinderProvider;

  requires com.google.auto.service;
  requires com.google.common;
  requires com.squareup.javapoet;
  requires flipkart.krystal.codegen.common;
  requires flipkart.krystal.lattice.codegen;
  requires flipkart.krystal.lattice.core;
  requires flipkart.krystal.vajram.codegen.common;
  requires jakarta.inject;
  requires org.checkerframework.checker.qual;
  requires jakarta.cdi;
  requires static lombok;
  requires flipkart.krystal.lattice.ext.cdi;
}
