module flipkart.krystal.vajram.codegen.common {
  exports com.flipkart.krystal.vajram.codegen.common.spi;
  exports com.flipkart.krystal.vajram.codegen.common.models;

  requires transitive flipkart.krystal.common;
  requires transitive java.compiler;
  requires transitive flipkart.krystal.vajram;
  requires com.google.common;
  requires com.squareup.javapoet;
  requires jakarta.inject;
  requires org.slf4j;
  requires org.checkerframework.checker.qual;
  requires static lombok;
  requires com.google.errorprone.annotations;
  requires flipkart.krystal.codegen.common;
  requires com.google.auto.service;
}
