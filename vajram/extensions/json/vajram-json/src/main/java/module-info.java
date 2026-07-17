module flipkart.krystal.vajram.ext.json {
  exports com.flipkart.krystal.vajram.json;
  exports com.flipkart.krystal.vajram.json.serialized;

  requires com.fasterxml.jackson.databind;
  requires com.fasterxml.jackson.datatype.guava;
  requires com.fasterxml.jackson.datatype.jdk8;
  requires com.fasterxml.jackson.datatype.jsr310;
  requires flipkart.krystal.common;
  requires com.fasterxml.jackson.module.paramnames;
  requires org.checkerframework.checker.qual;
  requires com.google.common;
  requires java.desktop;
  requires static lombok;
  requires com.google.auto.value.annotations;
  requires java.compiler;
  requires org.jspecify;
}
