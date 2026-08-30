package com.flipkart.krystal.vajram.lang.rust.plugin;

import java.io.IOException;
import java.util.Set;

/**
 * Extension point for source annotations on a Vajram's {@code callers} declaration.
 *
 * <p>Implementations register through {@link java.util.ServiceLoader}. An isolated processor is
 * called once with one matching Vajram; an aggregating processor is called once with every matching
 * Vajram in the compilation.
 */
public interface VajramAnnotationProcessor {

  Set<String> supportedAnnotations();

  ProcessingMode processingMode();

  void process(AnnotationProcessorContext context) throws IOException;
}
