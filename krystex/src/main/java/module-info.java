module flipkart.krystal.krystex {
  exports com.flipkart.krystal.krystex.batching;
  exports com.flipkart.krystal.krystex.caching;
  exports com.flipkart.krystal.krystex.commands;
  exports com.flipkart.krystal.krystex.dependencydecoration;
  exports com.flipkart.krystal.krystex.dependencydecorators;
  exports com.flipkart.krystal.krystex.kryon;
  exports com.flipkart.krystal.krystex.kryondecoration;
  exports com.flipkart.krystal.krystex.logicdecoration;
  exports com.flipkart.krystal.krystex;

  requires com.fasterxml.jackson.databind;
  requires com.fasterxml.jackson.datatype.jdk8;
  requires com.fasterxml.jackson.datatype.jsr310;
  requires com.google.common;
  requires flipkart.krystal.common;
  requires flipkart.krystal.vajram;
  requires jakarta.inject;
  requires java.compiler;
  requires org.checkerframework.checker.qual;
  requires static lombok;
  requires static org.slf4j;
}
