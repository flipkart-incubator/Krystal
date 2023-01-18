module flipkart.krystal.common {
  requires com.google.common;
  requires lombok;
  requires org.checkerframework.checker.qual;
  exports com.flipkart.krystal.data;
  exports com.flipkart.krystal.datatypes;
  exports com.flipkart.krystal.utils;
  exports com.flipkart.krystal.logic;
  exports com.flipkart.krystal.config;
}
