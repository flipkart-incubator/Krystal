package com.flipkart.krystal.vajram.lang.rust.ast;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A single input/facet declaration: grammar rules {@code input_id_declaration} (used standalone in
 * {@code inputs_list} and inside {@code assign_stat}) combined with the optional grouper and
 * annotations that precede it in {@code inputs_list}.
 */
public record InputDecl(
    @Nullable String grouper, List<String> annotations, TypeRef type, String name) {

  public static InputDecl of(TypeRef type, String name) {
    return new InputDecl(null, List.of(), type, name);
  }
}
