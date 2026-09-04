module flipkart.krystal.visualization {
  requires com.fasterxml.jackson.annotation;
  requires com.google.common;
  requires flipkart.krystal.common;
  requires flipkart.krystal.krystex;
  requires flipkart.krystal.vajram.ext.json;
  requires flipkart.krystal.vajram;
  requires org.checkerframework.checker.qual;
  requires org.slf4j;
  requires static lombok;
  requires tools.jackson.databind;

  exports com.flipkart.krystal.visualization.executiongraph;
}
