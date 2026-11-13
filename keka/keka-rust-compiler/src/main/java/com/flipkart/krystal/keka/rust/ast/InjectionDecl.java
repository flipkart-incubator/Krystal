package com.flipkart.krystal.keka.rust.ast;

import java.util.List;

/** Grammar rule {@code injection_id_declaration}: an injected dependency's type + name. */
public record InjectionDecl(List<VajramAnnotation> annotations, TypeRef type, String name) {}
