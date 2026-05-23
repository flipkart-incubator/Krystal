module krystal.vajram.ext.sql {
  exports com.flipkart.krystal.vajram.ext.sql.lang;
  exports com.flipkart.krystal.vajram.ext.sql.model;
  exports com.flipkart.krystal.vajram.ext.sql.lang.operators.comparison;
  exports com.flipkart.krystal.vajram.ext.sql.lang.operators.logical;

  requires flipkart.krystal.common;
  requires static lombok;
  requires com.google.auto.value.annotations;
  requires java.compiler;
}
