module flipkart.krystal.vajram.ext.sql.codegen {
  exports com.flipkart.krystal.vajram.sql.codegen;

  requires flipkart.krystal.vajram;
  requires r2dbc.spi;
  requires jakarta.inject;
  requires flipkart.krystal.codegen.common;
  requires flipkart.krystal.vajram.codegen.common;
  requires com.squareup.javapoet;
  requires org.reactivestreams;
  requires reactor.core;
  requires flipkart.krystal.vajram.ext.sql;
  requires com.google.common;
  requires static lombok;
  requires org.checkerframework.checker.qual;
  requires com.google.auto.value.annotations;
  requires jdk.compiler;
  requires com.google.auto.service;
}
