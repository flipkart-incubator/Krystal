module flipkart.krystal.krystex {
  exports com.flipkart.krystal.krystex to
      flipkart.krystal.vajramexecutor.krystex;
  exports com.flipkart.krystal.krystex.kryon to
      flipkart.krystal.vajramexecutor.krystex;
  exports com.flipkart.krystal.krystex.logicdecoration to
      flipkart.krystal.vajramexecutor.krystex;
  exports com.flipkart.krystal.krystex.logicdecorators.resilience4j to
      flipkart.krystal.vajramexecutor.krystex;
  exports com.flipkart.krystal.krystex.logicdecorators.observability to
      flipkart.krystal.vajramexecutor.krystex;
  exports com.flipkart.krystal.krystex.request to
      flipkart.krystal.vajramexecutor.krystex;
  exports com.flipkart.krystal.krystex.resolution to
      flipkart.krystal.vajramexecutor.krystex;
  exports com.flipkart.krystal.krystex.commands to
      flipkart.krystal.vajramexecutor.krystex;

  requires com.google.common;
  requires org.checkerframework.checker.qual;
  requires static io.github.resilience4j.all;
  requires static io.github.resilience4j.bulkhead;
  requires static io.github.resilience4j.circuitbreaker;
  requires static lombok;
  requires static org.slf4j;
  requires flipkart.krystal.common;
}
