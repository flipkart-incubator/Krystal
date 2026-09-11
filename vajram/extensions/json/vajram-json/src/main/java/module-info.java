module flipkart.krystal.vajram.ext.json {
  exports com.flipkart.krystal.vajram.json;
  exports com.flipkart.krystal.vajram.json.serialized;

  requires flipkart.krystal.common;
  requires org.checkerframework.checker.qual;
  requires com.google.common;
  requires java.desktop;
  requires static lombok;
  requires com.google.auto.value.annotations;
  requires java.compiler;
  requires org.jspecify;
  requires tools.jackson.core;
  requires tools.jackson.databind;
  requires tools.jackson.datatype.guava;
}
