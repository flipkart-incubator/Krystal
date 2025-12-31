package com.flipkart.krystal.lattice.ext.rest.dropwizard;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.concurrent.Flow.Publisher;

@Provider
@Produces(MediaType.TEXT_PLAIN)
@Singleton
public class FlowPublisherMessageBodyWriter implements MessageBodyWriter<ByteBuffer> {

  @Override
  public boolean isWriteable(
      Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    // Check if the type is a Publisher and media type is supported
    return Publisher.class.isAssignableFrom(type)
        && mediaType.isCompatible(MediaType.TEXT_PLAIN_TYPE);
  }

  @Override
  public void writeTo(
      ByteBuffer byteBuffer,
      Class<?> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, Object> httpHeaders,
      OutputStream entityStream)
      throws WebApplicationException {
    if (byteBuffer.hasArray()) {
      try {
        entityStream.write(byteBuffer.array());
        entityStream.flush();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
