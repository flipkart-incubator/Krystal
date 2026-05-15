module krystal.vajram.ext.sql {
  exports com.flipkart.krystal.vajram.ext.sql.statement;
  exports com.flipkart.krystal.vajram.ext.sql.model;

  requires flipkart.krystal.common;
  requires static lombok;
  requires com.google.auto.value.annotations;
  requires java.compiler;
}
