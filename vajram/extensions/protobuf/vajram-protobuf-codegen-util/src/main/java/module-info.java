import com.flipkart.krystal.codegen.common.datatypes.DataTypeFactory;
import com.flipkart.krystal.vajram.protobuf.codegen.util.types.ProtoDataTypeFactory;

module flipkart.krystal.vajram.ext.protobuf.codegen.util {
  exports com.flipkart.krystal.vajram.protobuf.codegen.util;
  exports com.flipkart.krystal.vajram.protobuf.codegen.util.types;

  requires flipkart.krystal.codegen.common;
  requires transitive flipkart.krystal.vajram.ext.protobuf.util;
  requires com.google.auto.service;
  requires org.slf4j;
  requires com.google.common;
  requires org.checkerframework.checker.qual;
  requires com.squareup.javapoet;
  requires static lombok;
  requires com.google.protobuf;
  requires flipkart.krystal.vajram.codegen.common;

  provides DataTypeFactory with
      ProtoDataTypeFactory;
}
