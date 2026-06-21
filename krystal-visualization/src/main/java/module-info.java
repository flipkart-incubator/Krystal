module flipkart.krystal.visualization {
  requires com.fasterxml.jackson.databind;
  requires com.fasterxml.jackson.datatype.jdk8;
  requires com.fasterxml.jackson.datatype.jsr310;
  requires com.google.common;
  requires flipkart.krystal.common;
  requires flipkart.krystal.krystex;
  requires flipkart.krystal.vajram;
  requires org.checkerframework.checker.qual;
  requires org.slf4j;
  requires static lombok;

  exports com.flipkart.krystal.visualization.executiongraph;
}
