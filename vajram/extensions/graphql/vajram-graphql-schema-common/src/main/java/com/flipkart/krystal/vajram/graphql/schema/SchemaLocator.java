package com.flipkart.krystal.vajram.graphql.schema;

import static com.flipkart.krystal.codegen.common.models.Constants.MODULE_ROOT_PATH_KEY;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javax.tools.StandardLocation;
import lombok.experimental.UtilityClass;

/**
 * Resolves the on-disk location of a GraphQL SDL schema resource, given a path relative to the
 * module root (e.g. {@code "src/main/graphqls/Schema.graphqls"}).
 */
@UtilityClass
public class SchemaLocator {

  /**
   * Returns the path to the schema file if found in SOURCE_PATH. If not found, it returns the path
   * relative to the module root path annotation processor option. It is the caller's responsibility
   * to check if the file at the returned path exists or not.
   */
  public static Path locate(CodeGenUtility util, String relativeSchemaPath) {
    try {
      return new File(
              util.processingEnv()
                  .getFiler()
                  .getResource(StandardLocation.SOURCE_PATH, "", relativeSchemaPath)
                  .toUri())
          .toPath();
    } catch (IOException e) {
      util.note(
          """
              Failed to get schema file '%s' in SOURCE_PATH. This can happen in projects which have not configured a JPMS named module. \
              Trying to look for 'moduleRootPath' annotation processor option"""
              .formatted(relativeSchemaPath));
      Path moduleRootPath = util.moduleRootPath();
      if (moduleRootPath == null) {
        throw new RuntimeException(
            "Schema file '"
                + relativeSchemaPath
                + "' was not present in SOURCE_PATH, nor was the "
                + MODULE_ROOT_PATH_KEY
                + " passed");
      }
      File schemaFile = moduleRootPath.resolve(relativeSchemaPath).toFile();
      if (!schemaFile.exists()) {
        util.note(
            "Schema file '"
                + relativeSchemaPath
                + "' was not present in SOURCE_PATH, nor was it found in the module path: "
                + schemaFile.getAbsolutePath());
      }
      return schemaFile.toPath();
    }
  }
}
