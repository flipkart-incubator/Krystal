package com.flipkart.krystal.vajram.lang.rust.codegen;

import java.nio.file.Path;

/** camelCase (vajram-lang identifiers) &lt;-&gt; snake_case (Rust module/fn conventions). */
public final class Naming {

  private Naming() {}

  public static String toSnakeCase(String camel) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < camel.length(); i++) {
      char c = camel.charAt(i);
      if (Character.isUpperCase(c)) {
        if (i > 0) {
          sb.append('_');
        }
        sb.append(Character.toLowerCase(c));
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  public static String capitalize(String s) {
    return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  public static String sourceModuleName(Path sourcePath) {
    String fileName = sourcePath.getFileName().toString();
    int extensionStart = fileName.lastIndexOf('.');
    return toSnakeCase(extensionStart == -1 ? fileName : fileName.substring(0, extensionStart));
  }
}
