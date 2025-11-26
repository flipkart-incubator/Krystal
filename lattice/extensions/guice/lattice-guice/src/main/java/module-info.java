module flipkart.krystal.lattice.ext.guice {
  exports com.flipkart.krystal.lattice.ext.guice;

  requires com.google.guice;
  requires static lombok;
  requires flipkart.krystal.lattice.core;
  requires com.google.common;
  requires jakarta.inject;
  requires org.checkerframework.checker.qual;
  requires flipkart.krystal.vajram;
  requires flipkart.krystal.vajram.guice;
}
