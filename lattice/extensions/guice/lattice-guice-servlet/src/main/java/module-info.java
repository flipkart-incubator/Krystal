module flipkart.krystal.lattice.ext.guice.servlet {
  exports com.flipkart.krystal.lattice.ext.guice.servlet;

  requires org.checkerframework.checker.qual;
  requires flipkart.krystal.vajram.guice;
  requires com.google.guice;
  requires com.google.common;
  requires jakarta.inject;
  requires flipkart.krystal.lattice.ext.guice;
  requires flipkart.krystal.vajram;
  requires flipkart.krystal.lattice.core;
  requires com.google.guice.extensions.servlet;
}
