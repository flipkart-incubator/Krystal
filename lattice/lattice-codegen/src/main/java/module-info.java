import com.flipkart.krystal.lattice.codegen.spi.LatticeCodeGeneratorProvider;

module flipkart.krystal.lattice.codegen {
  uses LatticeCodeGeneratorProvider;

  exports com.flipkart.krystal.lattice.codegen;
  exports com.flipkart.krystal.lattice.codegen.spi;
  exports com.flipkart.krystal.lattice.codegen.spi.di;

  requires com.google.auto.service;
  requires com.google.common;
  requires com.squareup.javapoet;
  requires flipkart.krystal.codegen.common;
  requires flipkart.krystal.lattice.core;
  requires flipkart.krystal.vajram.codegen.common;
  requires jakarta.inject;
  requires java.xml;
  requires org.checkerframework.checker.qual;
  requires static lombok;
  requires jakarta.cdi;
}
