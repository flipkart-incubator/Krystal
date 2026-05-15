module krystal.vajram.ext.sql.vertx {
  requires java.compiler;
  requires flipkart.krystal.vajram;
  requires io.vertx.core;
  requires io.vertx.sql.client;
  requires org.checkerframework.checker.qual;
  requires static lombok;
  requires org.slf4j;
  requires jakarta.inject;
  requires com.google.common;

  exports com.flipkart.krystal.vajram.ext.sql.vertx;
}
