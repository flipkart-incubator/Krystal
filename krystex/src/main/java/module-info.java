module flipkart.krystal.krystex {
  exports com.flipkart.krystal.krystex to
      flipkart.krystal.vajramexecutor.krystex;
  exports com.flipkart.krystal.krystex.node to flipkart.krystal.vajramexecutor.krystex;

  requires com.google.common;
  requires org.checkerframework.checker.qual;
  requires static io.github.resilience4j.all;
  requires static io.github.resilience4j.ratelimiter;
  requires static lombok;
  requires static org.slf4j;
  requires flipkart.krystal.common;
}
