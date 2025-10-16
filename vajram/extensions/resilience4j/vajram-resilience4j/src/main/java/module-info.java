module krystal.vajram.ext.resilience4j {
  exports com.flipkart.krystal.vajram.resilience4j.bulkhead;
  exports com.flipkart.krystal.vajram.resilience4j.curcuitbreaker;

  requires org.checkerframework.checker.qual;
  requires flipkart.krystal.common;
  requires io.github.resilience4j.all;
  requires flipkart.krystal.krystex;
  requires com.google.common;
  requires io.github.resilience4j.bulkhead;
  requires io.github.resilience4j.circuitbreaker;
}
