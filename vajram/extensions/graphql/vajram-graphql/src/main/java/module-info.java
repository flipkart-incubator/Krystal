module krystal.vajram.extensions.graphql {
  exports com.flipkart.krystal.vajram.graphql.api.model;
  exports com.flipkart.krystal.vajram.graphql.api.traits;
  exports com.flipkart.krystal.vajram.graphql.api.errors;
  exports com.flipkart.krystal.vajram.graphql.api.schema;
  exports com.flipkart.krystal.vajram.graphql.api.execution;

  requires flipkart.krystal.common;
  requires com.google.common;
  requires flipkart.krystal.krystex;
  requires com.graphqljava;
  requires flipkart.krystal.vajramexecutor.krystex;
  requires flipkart.krystal.vajram;
  requires org.checkerframework.checker.qual;
  requires com.graphqljava.extendedscalars;
  requires flipkart.krystal.vajram.ext.json;
  requires com.fasterxml.jackson.databind;
  requires static lombok;
  requires java.compiler;
  requires jakarta.inject;
  requires org.reflections;
}
