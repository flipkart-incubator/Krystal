module krystal.vajram.ext.sql.vertx {
  exports com.flipkart.krystal.vajram.ext.sql.vertx;

  requires com.google.common;
  requires flipkart.krystal.common;
  requires flipkart.krystal.vajram;
  requires io.vertx.client.sql.mysql;
  requires io.vertx.client.sql;
  requires io.vertx.core;
  requires jakarta.inject;
  requires java.compiler;
  requires org.checkerframework.checker.qual;
  requires org.slf4j;
  requires static lombok;
}
