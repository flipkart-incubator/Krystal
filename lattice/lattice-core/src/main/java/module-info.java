module flipkart.krystal.lattice.core {
  exports com.flipkart.krystal.lattice.core.di;
  exports com.flipkart.krystal.lattice.core.doping;
  exports com.flipkart.krystal.lattice.core.execution;
  exports com.flipkart.krystal.lattice.core.headers;
  exports com.flipkart.krystal.lattice.core;
  exports com.flipkart.krystal.lattice.vajram;

  requires org.slf4j;
  requires com.fasterxml.jackson.databind;
  requires com.fasterxml.jackson.dataformat.yaml;
  requires com.fasterxml.jackson.datatype.guava;
  requires com.fasterxml.jackson.datatype.jdk8;
  requires com.fasterxml.jackson.datatype.jsr310;
  requires com.google.auto.value.annotations;
  requires com.google.common;
  requires flipkart.krystal.common;
  requires flipkart.krystal.krystex;
  requires flipkart.krystal.vajram;
  requires flipkart.krystal.vajramexecutor.krystex;
  requires jakarta.inject;
  requires java.compiler;
  requires org.apache.commons.cli;
  requires org.checkerframework.checker.qual;
  requires static lombok;
}
