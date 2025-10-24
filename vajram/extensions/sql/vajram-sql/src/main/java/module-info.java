module flipkart.krystal.vajram.ext.sql {
  exports com.flipkart.krystal.vajram.sql;
  exports com.flipkart.krystal.vajram.sql.r2dbc;
  exports com.flipkart.krystal.vajram.sql.annotations;

  requires flipkart.krystal.vajram;
  requires jakarta.inject;
  requires r2dbc.pool;
  requires static lombok;
  requires org.checkerframework.checker.qual;
  requires com.google.auto.value.annotations;
  requires jdk.compiler;
  requires com.google.common;
  requires r2dbc.spi;
  requires reactor.core;
  requires org.reactivestreams;
}
