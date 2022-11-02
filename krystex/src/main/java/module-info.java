module flipkart.krystal.krystex {
  exports com.flipkart.krystal.krystex to
      flipkart.krystal.vajram;

  requires com.google.common;
  requires io.github.resilience4j.all;
  requires io.github.resilience4j.ratelimiter;
  requires lombok;
}
