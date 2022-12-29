package com.flipkart.krystal.vajram.codegen;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import org.junit.jupiter.api.Test;

class VajramCodeGeneratorTest {

  public static final String SOURCE_PATH =
      "/Users/ram.anvesh/Personal/codebase/flipkart-incubator/Krystal/"
          + "vajram/vajram-codegen/src/main/java/com/flipkart/krystal/vajram/codegen/samples/hello";
  private static final String BINARY_PATH =
      "/Users/ram.anvesh/Personal/codebase/flipkart-incubator/Krystal/vajram/vajram-codegen/build/classes/"
          + "java/main/com/flipkart/krystal/vajram/codegen/samples/hello";

  @Test
  void testCodeGen() throws Exception {
//    VajramCodeGenTask.getVajrams(
//            new URLClassLoader(new URL[] {new File(BINARY_PATH).toURI().toURL()}))
//        .forEach(
//            vajram ->
//                System.out.println(new VajramCodeGenerator(vajram).codeGenVajramRequest()));
  }
}
