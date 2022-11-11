module flipkart.krystal.vajram {
  exports com.flipkart.krystal.vajram.inputs;
  exports com.flipkart.krystal.vajram;
  exports com.flipkart.krystal.datatypes;
  exports com.flipkart.krystal.vajram.das;

  requires flipkart.krystal.krystex;
  requires com.google.common;
  requires static lombok;
  requires org.reflections;
  requires org.checkerframework.checker.qual;
  requires com.google.errorprone.annotations;
}
