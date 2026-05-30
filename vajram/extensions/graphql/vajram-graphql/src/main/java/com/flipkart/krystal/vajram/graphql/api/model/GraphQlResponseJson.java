package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.annos.NoAnnotation;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.array.ByteArray;
import com.flipkart.krystal.serial.SerdeProtocol;
import com.flipkart.krystal.serial.SerializableModel;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class GraphQlResponseJson implements SerdeProtocol<NoAnnotation, SerializableModel> {

  public static final GraphQlResponseJson INSTANCE = new GraphQlResponseJson();

  @Override
  public String modelClassesSuffix() {
    return "GQlRespJson";
  }

  @Override
  public String defaultContentType() {
    return "application/graphql-response+json";
  }

  @Override
  public ByteArray serialize(
      Object object,
      Function<Model, SerializableModel> modelMapper,
      @Nullable NoAnnotation customConfig) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T deserialize(Object payload, Object typeInfo, @Nullable NoAnnotation customConfig) {
    throw new UnsupportedOperationException();
  }

  private GraphQlResponseJson() {}
}
