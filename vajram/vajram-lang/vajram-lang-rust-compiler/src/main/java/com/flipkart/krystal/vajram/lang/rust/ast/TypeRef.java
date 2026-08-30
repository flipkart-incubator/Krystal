package com.flipkart.krystal.vajram.lang.rust.ast;

import java.util.List;

/**
 * Grammar rule {@code type} (plus {@code non_param_type} and {@code errableType}): a possibly
 * generic, possibly errable/soon type reference, e.g. {@code Set<string>}, {@code ProductDetails?},
 * {@code Map<#batch, ProductDetails>~}.
 */
public record TypeRef(
    String name, List<TypeRef> typeArgs, boolean grouperType, boolean errable, boolean soon) {

  public static TypeRef simple(String name) {
    return new TypeRef(name, List.of(), false, false, false);
  }
}
