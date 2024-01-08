module flipkart.krystal.vajram {
  exports com.flipkart.krystal.vajram.inputs;
  exports com.flipkart.krystal.vajram.inputs.resolution;
  exports com.flipkart.krystal.vajram;
  exports com.flipkart.krystal.vajram.das;
  exports com.flipkart.krystal.vajram.modulation;
  exports com.flipkart.krystal.vajram.exec;
  exports com.flipkart.krystal.vajram.tags;
  exports com.flipkart.krystal.vajram.exception;

  requires com.google.common;
  requires static lombok;
  requires org.reflections;
  requires org.checkerframework.checker.qual;
  requires com.google.errorprone.annotations;
  requires flipkart.krystal.common;
  requires static org.slf4j;
}
