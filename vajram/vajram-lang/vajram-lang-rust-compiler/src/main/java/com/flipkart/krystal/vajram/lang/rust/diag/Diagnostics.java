package com.flipkart.krystal.vajram.lang.rust.diag;

import com.flipkart.krystal.vajram.lang.rust.ast.SourceLocation;
import java.util.ArrayList;
import java.util.List;

/** Mutable sink shared by every compiler pass (parse/AST-build/resolve/emit). */
public final class Diagnostics {

  private final List<Diagnostic> diagnostics = new ArrayList<>();

  public void error(SourceLocation location, String message) {
    diagnostics.add(Diagnostic.error(location, message));
  }

  public void warning(SourceLocation location, String message) {
    diagnostics.add(Diagnostic.warning(location, message));
  }

  public List<Diagnostic> all() {
    return List.copyOf(diagnostics);
  }

  public boolean hasErrors() {
    return diagnostics.stream().anyMatch(d -> d.severity() == Diagnostic.Severity.ERROR);
  }

  public int errorCount() {
    return (int)
        diagnostics.stream().filter(d -> d.severity() == Diagnostic.Severity.ERROR).count();
  }
}
