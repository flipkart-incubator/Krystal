module flipkart.krystal.vajramDef {
  exports com.flipkart.krystal.vajramDef.facets;
  exports com.flipkart.krystal.vajramDef.facets.resolution;
  exports com.flipkart.krystal.vajramDef;
  exports com.flipkart.krystal.vajramDef.batching;
  exports com.flipkart.krystal.vajramDef.exec;
  exports com.flipkart.krystal.vajramDef.exception;
  exports com.flipkart.krystal.vajramDef.utils;
  exports com.flipkart.krystal.vajramDef.facets.specs;
  exports com.flipkart.krystal.vajramDef.annos;
  exports com.flipkart.krystal.vajramDef.inputinjection;
  exports com.flipkart.krystal.vajramDef.traitbinding;
  exports com.flipkart.krystal.vajram;
  exports com.flipkart.krystal.vajram.facets;
  exports com.flipkart.krystal.vajram.exec;
  exports com.flipkart.krystal.vajram.traitbinding;

  requires com.google.common;
  requires static lombok;
  requires org.reflections;
  requires org.checkerframework.checker.qual;
  requires com.google.errorprone.annotations;
  requires flipkart.krystal.common;
  requires static org.slf4j;
}
