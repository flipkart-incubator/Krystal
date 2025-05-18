module flipkart.krystal.lattice.core {
  requires flipkart.krystal.common;
  requires com.google.auto.value.annotations;
  requires static lombok;
  requires flipkart.krystal.vajramexecutor.krystex;
  requires com.google.common;
  requires org.apache.commons.cli;
  requires jakarta.inject;
  requires org.checkerframework.checker.qual;
  requires com.fasterxml.jackson.databind;
  requires org.slf4j;
  requires flipkart.krystal.krystex;
  requires com.fasterxml.jackson.datatype.jsr310;
  requires com.fasterxml.jackson.datatype.jdk8;
  requires com.fasterxml.jackson.dataformat.yaml;
  requires com.fasterxml.jackson.datatype.guava;

  exports com.flipkart.krystal.lattice.core;
  exports com.flipkart.krystal.lattice.core.vajram;
  exports com.flipkart.krystal.lattice.core.annos;
}
