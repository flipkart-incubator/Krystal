package com.flipkart.krystal.vajram.lang.rust.system;

import com.flipkart.krystal.vajram.lang.rust.ast.Completion;
import com.flipkart.krystal.vajram.lang.rust.ast.VajramFile;
import java.util.List;
import java.util.Optional;

/** Built-in Vajrams supplied by the runtime rather than by a source file. */
public enum SystemVajram {
  READ_FILE_AS_STRING("readFileAsString", List.of("lang", "FileSystem"), Completion.SOON),
  CONCAT_STRINGS("concatStrings", List.of("lang", "Strings"), Completion.NOW);

  private final String name;
  private final List<String> sourceSegments;
  private final Completion completion;

  SystemVajram(String name, List<String> sourceSegments, Completion completion) {
    this.name = name;
    this.sourceSegments = sourceSegments;
    this.completion = completion;
  }

  public Completion completion() {
    return completion;
  }

  public static Optional<SystemVajram> lookup(String name, List<VajramFile.ImportDecl> imports) {
    for (SystemVajram systemVajram : values()) {
      if (systemVajram.name.equals(name)
          && imports.stream()
              .anyMatch(
                  imported ->
                      imported.vajramName().equals(name)
                          && imported.sourceSegments().equals(systemVajram.sourceSegments))) {
        return Optional.of(systemVajram);
      }
    }
    return Optional.empty();
  }
}
