package com.flipkart.krystal.keka.rust.ast;

import java.util.List;

/** Grammar rule {@code injection_id_declaration}: an injected dependency's type + name. */
public record InjectionDecl(List<String> annotations, TypeRef type, String name) {}
