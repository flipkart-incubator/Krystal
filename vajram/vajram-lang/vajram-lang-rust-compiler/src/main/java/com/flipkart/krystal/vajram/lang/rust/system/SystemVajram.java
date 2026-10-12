package com.flipkart.krystal.vajram.lang.rust.system;

import com.flipkart.krystal.vajram.lang.rust.ast.Completion;
import com.flipkart.krystal.vajram.lang.rust.ast.VajramFile;
import com.flipkart.krystal.vajram.lang.rust.cli.RustCompilerMain.Target;
import java.util.List;
import java.util.Optional;

/** Built-in Vajrams supplied by the runtime rather than by a source file. */
public enum SystemVajram {
  READ_FILE_AS_STRING("readFileAsString", List.of("lang", "fileSystem"), Completion.SOON),
  CONCAT_STRINGS("concatStrings", List.of("lang", "strings"), Completion.NOW),
  CALL_HTTP("callHttp", List.of("lang", "net"), Completion.SOON);

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

  /** Returns whether this runtime-provided capability can be emitted for the selected target. */
  public boolean supports(Target target) {
    return switch (this) {
      case READ_FILE_AS_STRING -> target == Target.NATIVE;
      case CONCAT_STRINGS, CALL_HTTP -> true;
    };
  }

  /** Explains why {@link #supports} rejected the wasm target, for use in diagnostics. */
  public String unsupportedTargetReason() {
    return switch (this) {
      case READ_FILE_AS_STRING -> "browser file-picker support is not bundled";
      case CONCAT_STRINGS, CALL_HTTP -> "unsupported";
    };
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
