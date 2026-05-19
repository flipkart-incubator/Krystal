package com.flipkart.krystal.vajram.fory;

import com.flipkart.krystal.serial.SerdeProtocol;
import org.apache.fory.config.Language;

/**
 * {@link SerdeProtocol} implementation backed by <a href="https://fory.apache.org/">Apache Fory</a>
 * — a blazing-fast, JIT-compiled, cross-language serialization framework.
 *
 * <p>Unlike schema-based protocols such as protobuf, Fory serializes plain Java object graphs
 * directly. No IDL files are needed; the JIT compiler generates optimised (de)serializers at
 * runtime. This makes Fory ideal for same-language high-throughput pipelines where schema evolution
 * is managed at the application level.
 *
 * <p>Usage: annotate a {@code @ModelRoot} with {@code @SupportedModelProtocols(Fory.class)} and the
 * Krystal codegen will produce an {@code _ImmutFory} wrapper whose {@code _serialize()} /
 * constructor-from-bytes round-trip through this protocol.
 */
public final class Fory implements SerdeProtocol {

  public static final Fory FORY = new Fory();

  private static final org.apache.fory.Fory FORY_INSTANCE =
      org.apache.fory.Fory.builder()
          .withLanguage(Language.JAVA)
          // Allow serializing types that are not explicitly registered.
          // Generated _ImmutFory classes register themselves for performance,
          // but this keeps the framework flexible.
          .requireClassRegistration(false)
          .build();

  /**
   * Returns the shared Fory instance used for Krystal model serialization. Krystal models are not
   * thread-safe by design, so no thread-safety overhead is added here.
   */
  public static org.apache.fory.Fory foryInstance() {
    return FORY_INSTANCE;
  }

  @Override
  public String modelClassesSuffix() {
    return "Fory";
  }

  @Override
  public String defaultContentType() {
    return "application/x-fory";
  }

  private Fory() {}
}
