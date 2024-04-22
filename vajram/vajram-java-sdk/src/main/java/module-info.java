module flipkart.krystal.vajram {
  exports com.flipkart.krystal.vajram.facets;
  exports com.flipkart.krystal.vajram.facets.resolution;
  exports com.flipkart.krystal.vajram;
  exports com.flipkart.krystal.vajram.das;
  exports com.flipkart.krystal.vajram.batching;
  exports com.flipkart.krystal.vajram.exec;
  exports com.flipkart.krystal.vajram.tags;
  exports com.flipkart.krystal.vajram.exception;
  exports com.flipkart.krystal.vajram.facets.resolution.sdk;
  exports com.flipkart.krystal.vajram.utils;

  requires com.google.common;
  requires static lombok;
  requires org.reflections;
  requires org.checkerframework.checker.qual;
  requires com.google.errorprone.annotations;
  requires flipkart.krystal.common;
  requires static org.slf4j;
}
