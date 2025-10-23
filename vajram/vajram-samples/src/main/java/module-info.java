module flipkart.krystal.vajram.vajram_samples {
  requires flipkart.krystal.vajram;
  requires com.google.common;
  requires java.logging;
  requires org.checkerframework.checker.qual;
  requires jakarta.inject;
  requires jdk.compiler;
  requires com.google.auto.value.annotations;
  requires static lombok;

  // SQL extension requirements
  requires flipkart.krystal.vajram.ext.sql;
  requires r2dbc.spi;
  //requires r2dbc.pool;
  requires reactor.core;
}
