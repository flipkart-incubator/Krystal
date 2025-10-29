package com.flipkart.krystal.vajram.graphql.codegen;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import java.io.File;
import java.io.IOException;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

public final class GraphQlCodeGenUtil {

  private final CodeGenUtility util;

  public GraphQlCodeGenUtil(CodeGenUtility util) {
    this.util = util;
  }

  File getSchemaFile() {
    FileObject schemaFileObject;
    try {
      schemaFileObject =
          util.processingEnv()
              .getFiler()
              .getResource(StandardLocation.SOURCE_PATH, "", "Schema.graphqls");
      return new File(schemaFileObject.toUri());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
