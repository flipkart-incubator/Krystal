module flipkart.krystal.lattice.ext.cdi {
  exports com.flipkart.krystal.lattice.ext.cdi;

  requires org.checkerframework.checker.qual;
  requires com.google.common;
  requires jakarta.inject;
  requires flipkart.krystal.vajram;
  requires flipkart.krystal.lattice.core;
  requires jakarta.cdi;
  requires static lombok;
  requires org.slf4j;
  requires krystal.vajram.ext.cdi;
}
