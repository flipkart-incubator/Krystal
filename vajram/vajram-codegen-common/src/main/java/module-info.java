module flipkart.krystal.vajram.codegen.common {
  exports com.flipkart.krystal.vajram.codegen.common.spi;
  exports com.flipkart.krystal.vajram.codegen.common.models;

  requires flipkart.krystal.vajram;
  requires com.google.common;
  requires com.squareup.javapoet;
  requires jakarta.inject;
  requires java.compiler;
  requires org.slf4j;
  requires static lombok;
}
