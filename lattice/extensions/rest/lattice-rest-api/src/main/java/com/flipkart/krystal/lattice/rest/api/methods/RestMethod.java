package com.flipkart.krystal.lattice.rest.api.methods;

import java.lang.annotation.Annotation;
import lombok.Getter;

@Getter
public enum RestMethod {
  GET(GET.class, jakarta.ws.rs.GET.class),
  POST(POST.class, jakarta.ws.rs.POST.class),
  PUT(PUT.class, jakarta.ws.rs.PUT.class),
  DELETE(DELETE.class, jakarta.ws.rs.DELETE.class);

  private final Class<? extends Annotation> latticeAnnotation;
  private final Class<? extends Annotation> jakartaAnnotation;

  RestMethod(
      Class<? extends Annotation> latticeAnnotation,
      Class<? extends Annotation> jakartaAnnotation) {
    this.latticeAnnotation = latticeAnnotation;
    this.jakartaAnnotation = jakartaAnnotation;
  }

  public boolean supportsRequestBody() {
    return this != GET;
  }
}
