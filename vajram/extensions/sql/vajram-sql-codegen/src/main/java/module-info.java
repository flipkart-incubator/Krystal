module krystal.vajram.ext.sql.codegen {
  requires krystal.vajram.ext.sql;
  requires flipkart.krystal.codegen.common;
  requires java.compiler;
  requires org.checkerframework.checker.qual;
  requires flipkart.krystal.vajram;
  requires flipkart.krystal.vajram.codegen.common;
  requires com.google.common;

  exports com.flipkart.krystal.vajram.ext.sql.codegen;
}
