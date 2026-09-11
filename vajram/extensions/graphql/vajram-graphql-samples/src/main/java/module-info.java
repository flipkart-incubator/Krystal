import com.flipkart.krystal.vajram.graphql.api.GraphQlModule;

@GraphQlModule
module krystal.vajram.ext.graphql.samples {
  requires com.fasterxml.jackson.annotation;
  requires com.google.common;
  requires com.graphqljava;
  requires flipkart.krystal.krystex;
  requires flipkart.krystal.vajram.ext.json;
  requires flipkart.krystal.vajram;
  requires java.compiler;
  requires krystal.vajram.ext.graphql.client;
  requires krystal.vajram.extensions.graphql;
  requires org.checkerframework.checker.qual;
  requires org.slf4j;
  requires static lombok;
  requires tools.jackson.databind;
}
