module krystal.vajram.ext.cdi {
  exports com.flipkart.krystal.vajram.ext.cdi.injection;

  requires flipkart.krystal.vajram;
  requires jakarta.inject;
  requires jakarta.cdi;
  requires static lombok;
  requires org.slf4j;
}
