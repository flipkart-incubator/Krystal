module flipkart.krystal.common {
  requires com.google.common;
  requires static lombok;
  requires org.checkerframework.checker.qual;
  requires java.compiler;
  requires java.xml;

  exports com.flipkart.krystal.data;
  exports com.flipkart.krystal.facets;
  exports com.flipkart.krystal.datatypes;
  exports com.flipkart.krystal.utils;
  exports com.flipkart.krystal.config;
  exports com.flipkart.krystal.schema;
  exports com.flipkart.krystal.except;
  exports com.flipkart.krystal.resolution;
}
