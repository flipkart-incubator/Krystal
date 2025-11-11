module flipkart.krystal.vajram.guice {
  exports com.flipkart.krystal.vajram.guice.injection;

  requires com.google.guice;
  requires flipkart.krystal.vajramexecutor.krystex;
  requires flipkart.krystal.vajram;
  requires flipkart.krystal.common;
  requires jakarta.inject;
  requires com.google.common;
  requires org.slf4j;
  requires static lombok;
  requires org.checkerframework.checker.qual;
  requires flipkart.krystal.krystex;
}
