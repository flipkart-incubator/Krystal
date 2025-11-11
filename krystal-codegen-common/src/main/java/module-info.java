module flipkart.krystal.codegen.common {
  exports com.flipkart.krystal.codegen.common.spi;
  exports com.flipkart.krystal.codegen.common.models;
  exports com.flipkart.krystal.codegen.common.datatypes;

  requires transitive flipkart.krystal.common;
  requires transitive java.compiler;
  requires com.google.common;
  requires com.squareup.javapoet;
  requires jakarta.inject;
  requires org.slf4j;
  requires org.checkerframework.checker.qual;
  requires static lombok;
  requires com.google.errorprone.annotations;
  requires jdk.jshell;
}
