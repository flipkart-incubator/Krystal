package com.flipkart.krystal.vajram.graphql.codegen;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.IfAbsent.IfAbsentThen;
import com.squareup.javapoet.AnnotationSpec;
import java.nio.file.Path;

public class CodeGenConstants {

  public static final Path GRAPHQL_SRC_DIR = Path.of("src", "main", "graphqls");

  static final AnnotationSpec IF_ABSENT_FAIL =
      AnnotationSpec.builder(IfAbsent.class)
          .addMember("value", "$T.$L", IfAbsentThen.class, "FAIL")
          .build();
}
