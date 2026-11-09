package com.flipkart.krystal.keka.rust.ast;

import java.util.List;

/**
 * A single input/facet declaration: grammar rules {@code input_declaration} (in {@code
 * inputs_list}) and {@code local_declaration} (inside {@code assign_stat}), with the annotations
 * that precede it (e.g. {@code `batched} on an input).
 */
public record InputDecl(List<String> annotations, TypeRef type, String name) {

  public static InputDecl of(TypeRef type, String name) {
    return new InputDecl(List.of(), type, name);
  }
}
