package com.flipkart.krystal.vajram.lang.rust.codegen;

/**
 * Minimal indentation-aware text builder for emitting Rust source. Rust source is plain text, so a
 * StringBuilder-based writer is all codegen needs here - no templating engine or "javapoet for
 * Rust" library exists or is warranted for this.
 */
public final class RustWriter {

  private final StringBuilder out = new StringBuilder();
  private int indent = 0;
  private boolean atLineStart = true;

  public RustWriter line(String text) {
    indentIfNeeded();
    out.append(text).append('\n');
    atLineStart = true;
    return this;
  }

  public RustWriter blank() {
    out.append('\n');
    atLineStart = true;
    return this;
  }

  public RustWriter append(String text) {
    indentIfNeeded();
    out.append(text);
    return this;
  }

  public RustWriter openBlock(String header) {
    line(header + " {");
    indent++;
    return this;
  }

  public RustWriter closeBlock() {
    indent--;
    line("}");
    return this;
  }

  public RustWriter closeBlockAs(String suffix) {
    indent--;
    line("}" + suffix);
    return this;
  }

  private void indentIfNeeded() {
    if (atLineStart) {
      out.append("    ".repeat(Math.max(indent, 0)));
      atLineStart = false;
    }
  }

  @Override
  public String toString() {
    return out.toString();
  }
}
