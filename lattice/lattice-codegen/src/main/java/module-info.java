module flipkart.krystal.lattice.codegen {
  exports com.flipkart.krystal.lattice.codegen;
  exports com.flipkart.krystal.lattice.codegen.spi;

  requires com.google.auto.common;
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
}
