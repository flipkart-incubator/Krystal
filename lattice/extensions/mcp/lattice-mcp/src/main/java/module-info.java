module krystal.lattice.ext.mcp {
  exports com.flipkart.krystal.lattice.ext.mcp.api;
  exports com.flipkart.krystal.lattice.ext.mcp;

  requires flipkart.krystal.lattice.core;
  requires flipkart.krystal.vajram;
  requires org.checkerframework.checker.qual;
  requires static lombok;
  requires jakarta.inject;
  requires org.jspecify;
}
