module krystal.vajram.ext.sql.vertx.codegen {
  requires flipkart.krystal.vajram;
  requires flipkart.krystal.codegen.common;
  requires flipkart.krystal.vajram.codegen.common;
  requires krystal.vajram.ext.sql;
  requires krystal.vajram.ext.sql.codegen;
  requires krystal.vajram.ext.sql.vertx;
  requires io.vertx.sql.client;
  requires com.google.common;
  requires com.squareup.javapoet;
  requires com.google.auto.service;
  requires java.compiler;
  requires static lombok;
  requires org.checkerframework.checker.qual;
}
