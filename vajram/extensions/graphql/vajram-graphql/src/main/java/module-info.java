module krystal.vajram.extensions.graphql {
  exports com.flipkart.krystal.vajram.graphql.api;

  requires flipkart.krystal.common;
  requires com.google.common;
  requires flipkart.krystal.krystex;
  requires com.graphqljava;
  requires flipkart.krystal.vajramexecutor.krystex;
  requires flipkart.krystal.vajram;
  requires org.checkerframework.checker.qual;
  requires com.graphqljava.extendedscalars;
  requires static lombok;
}
