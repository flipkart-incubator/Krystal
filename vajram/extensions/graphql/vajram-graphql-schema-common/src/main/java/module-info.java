module krystal.vajram.ext.graphql.schema.common {
  exports com.flipkart.krystal.vajram.graphql.schema;

  requires com.google.common;
  requires static lombok;
  requires com.squareup.javapoet;
  requires com.graphqljava;
  requires org.checkerframework.checker.qual;
  requires flipkart.krystal.codegen.common;
  requires org.slf4j;
}
