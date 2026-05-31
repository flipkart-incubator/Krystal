package com.flipkart.krystal.vajram.fory;

import com.flipkart.krystal.annos.NoAnnotation;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.array.ByteArray;
import com.flipkart.krystal.model.array.SimpleByteArray;
import com.flipkart.krystal.serial.SerdeProtocol;
import com.flipkart.krystal.serial.SerializableModel;
import java.util.ServiceLoader;
import java.util.function.Function;
import org.apache.fory.ThreadSafeFory;
import org.apache.fory.config.Language;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * {@link SerdeProtocol} implementation backed by <a href="https://fory.apache.org/">Apache Fory</a>
 * — a blazing-fast, JIT-compiled, cross-language serialization framework.
 *
 * <p>Unlike schema-based protocols such as protobuf, Fory serializes plain Java object graphs
 * directly. No IDL files are needed; the JIT compiler generates optimised (de)serializers at
 * runtime. This makes Fory ideal for same-language high-throughput pipelines where schema evolution
 * is managed at the application level.
 *
 * <p>Usage: annotate a {@code @ModelRoot} with {@code @SupportedModelProtocol(Fory.class)} and the
 * Krystal codegen will produce an {@code _ImmutFory} wrapper whose {@code _serialize()} /
 * constructor-from-bytes round-trip through this protocol.
 */
public final class Fory implements SerdeProtocol<NoAnnotation, SerializableModel> {

  private static final ThreadSafeFory FORY_INSTANCE =
      org.apache.fory.Fory.builder()
          .withLanguage(Language.JAVA)
          // Allow serializing types that are not explicitly registered.
          // Generated _ImmutFory classes register themselves for performance,
          // but this keeps the framework flexible.
          .requireClassRegistration(false)
          .registerGuavaTypes(true)
          .buildThreadSafeFory();

  public static final Fory FORY = new Fory();

  /**
   * Returns the shared Fory instance used for Krystal model serialization. Krystal models are not
   * thread-safe by design, so no thread-safety overhead is added here.
   */
  public static ThreadSafeFory foryInstance() {
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

  @Override
  public ByteArray serialize(
      Object object,
      Function<Model, SerializableModel> modelMapper,
      @Nullable NoAnnotation customConfig) {
    return SimpleByteArray.of(FORY_INSTANCE.serialize(object));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T deserialize(Object payload, Object typeInfo, @Nullable NoAnnotation customConfig) {
    if (typeInfo instanceof Class<?> clazz) {
      if (payload instanceof byte[] bytes) {
        return (T) FORY_INSTANCE.deserialize(bytes, clazz);
      } else if (payload instanceof ByteArray byteArray) {
        return (T) FORY_INSTANCE.deserialize(byteArray.toArray(), clazz);
      }
    }
    throw new UnsupportedOperationException(
        "Cannot deserialize payload of type " + payload.getClass() + " for typeInfo " + typeInfo);
  }

  private Fory() {
    ServiceLoader.load(ForyClassProvider.class)
        .forEach(provider -> FORY_INSTANCE.register(provider.getForyClass()));
  }

  public interface ForyClassProvider {
    Class<? extends SerializableForyModel> getForyClass();
  }
}
