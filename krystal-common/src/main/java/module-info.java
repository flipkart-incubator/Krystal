module flipkart.krystal.common {
  exports com.flipkart.krystal.data;
  exports com.flipkart.krystal.facets;
  exports com.flipkart.krystal.datatypes;
  exports com.flipkart.krystal.config;
  exports com.flipkart.krystal.except;
  exports com.flipkart.krystal.concurrent;
  exports com.flipkart.krystal.pooling;
  exports com.flipkart.krystal.tags;
  exports com.flipkart.krystal.annos;
  exports com.flipkart.krystal.facets.resolution;
  exports com.flipkart.krystal.core;
  exports com.flipkart.krystal.traits;
  exports com.flipkart.krystal.traits.matchers;
  exports com.flipkart.krystal.serial;
  exports com.flipkart.krystal.model;

  requires com.google.common;
  requires org.checkerframework.checker.qual;
  requires java.compiler;
  requires org.slf4j;
  requires jdk.httpserver;
  requires com.google.auto.value.annotations;
  requires jakarta.inject;
  requires static lombok;
}
