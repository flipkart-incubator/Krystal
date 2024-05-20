package com.flipkart.krystal.vajram.tags;

import java.lang.annotation.Annotation;

public record AnnotationTagKey(Object key, Class<? extends Annotation> annotationType) {}
