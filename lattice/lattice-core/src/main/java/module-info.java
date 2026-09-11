module flipkart.krystal.lattice.core {
  exports com.flipkart.krystal.lattice.core.di;
  exports com.flipkart.krystal.lattice.core.doping;
  exports com.flipkart.krystal.lattice.core.execution;
  exports com.flipkart.krystal.lattice.core.headers;
  exports com.flipkart.krystal.lattice.core;
  exports com.flipkart.krystal.lattice.vajram;
  exports com.flipkart.krystal.lattice.krystex;

  requires org.slf4j;
  requires com.google.auto.value.annotations;
  requires com.google.common;
  requires flipkart.krystal.common;
  requires flipkart.krystal.krystex;
  requires flipkart.krystal.vajram;
  requires jakarta.inject;
  requires java.compiler;
  requires org.apache.commons.cli;
  requires org.checkerframework.checker.qual;
  requires static lombok;
  requires jakarta.cdi;
  requires com.fasterxml.jackson.annotation;
  requires tools.jackson.dataformat.yaml;
  requires tools.jackson.databind;
  requires tools.jackson.datatype.guava;
}
