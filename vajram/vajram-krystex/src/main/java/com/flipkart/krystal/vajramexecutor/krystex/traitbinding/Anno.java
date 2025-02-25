package com.flipkart.krystal.vajramexecutor.krystex.traitbinding;

import java.lang.annotation.Annotation;

sealed interface Anno {
  final record Unqualified() implements Anno {}

  final record AnnoObject(Annotation annotation) implements Anno {}

  final record AnnoClass(Class<? extends Annotation> annotationType) implements Anno {}
}
