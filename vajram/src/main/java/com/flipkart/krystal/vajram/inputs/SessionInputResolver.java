package com.flipkart.krystal.vajram.inputs;

import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.datatypes.JavaDataType;
import com.flipkart.krystal.vajram.adaptors.DependencyInjectionAdaptor;
import java.lang.reflect.Type;
import java.util.Optional;

public record SessionInputResolver(JavaDataType<?> dataType, String annotation) {

  public ValueOrError<Object> resolve(DependencyInjectionAdaptor<?> injectionAdaptor) {
    if (injectionAdaptor == null || injectionAdaptor.getInjector() == null) {
      return ValueOrError.withError(
          new Exception("Dependency Injection null, cannot resolve SESSION input"));
    }

    if (dataType == null || dataType.javaType().isEmpty()) {
      return ValueOrError.withError(new Exception("Data type is null, cannot inject"));
    }
    Optional<Type> type = dataType.javaType();
    Object resolvedObject = null;
    if (annotation != null) {
      resolvedObject = injectionAdaptor.getInstance((Class<?>) type.get(), annotation);
    }
    if (resolvedObject == null) {
      resolvedObject = injectionAdaptor.getInstance(((Class<?>) type.get()));
    }
    return ValueOrError.withValue(resolvedObject);
  }
}
