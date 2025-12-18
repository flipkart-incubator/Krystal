module flipkart.krystal.vajram {
  exports com.flipkart.krystal.vajram.facets;
  exports com.flipkart.krystal.vajram.facets.resolution;
  exports com.flipkart.krystal.vajram;
  exports com.flipkart.krystal.vajram.batching;
  exports com.flipkart.krystal.vajram.exec;
  exports com.flipkart.krystal.vajram.exception;
  exports com.flipkart.krystal.vajram.utils;
  exports com.flipkart.krystal.vajram.facets.specs;
  exports com.flipkart.krystal.vajram.annos;
  exports com.flipkart.krystal.vajram.inputinjection;

  requires transitive flipkart.krystal.common;
  requires com.google.common;
  requires org.reflections;
  requires org.checkerframework.checker.qual;
  requires com.google.errorprone.annotations;
  requires java.compiler;
  requires com.google.auto.value.annotations;
  requires static lombok;
  requires static org.slf4j;
  requires jakarta.inject;
}
