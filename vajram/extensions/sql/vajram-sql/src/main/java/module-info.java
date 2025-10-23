module flipkart.krystal.vajram.ext.sql {
  exports com.flipkart.krystal.vajram.sql;
  exports com.flipkart.krystal.vajram.sql.r2dbc;

  // requires transitive flipkart.krystal.common;
  requires flipkart.krystal.vajram;
  requires r2dbc.spi;
  requires jakarta.inject;
  requires r2dbc.pool;
  //  requires reactor.core;
  requires org.reactivestreams;
  requires reactor.core;
  //  requires r2dbc.mysql;
  requires static lombok;
  requires org.checkerframework.checker.qual;
  requires com.google.auto.value.annotations;
  requires jdk.compiler;
  requires com.google.common;
  requires java.logging;
}
