module flipkart.krystal.vajram.ext.sql.codegen {
  exports com.flipkart.krystal.vajram.sql.codegen;

  requires flipkart.krystal.codegen.common;
  requires flipkart.krystal.vajram.codegen.common;
  requires com.squareup.javapoet;
  requires flipkart.krystal.vajram.ext.sql;
  requires com.google.auto.service;
  requires com.google.common;
  requires org.checkerframework.checker.qual;
}
