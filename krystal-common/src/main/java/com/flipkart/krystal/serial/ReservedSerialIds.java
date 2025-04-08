package com.flipkart.krystal.serial;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * {@link SerialId}s which are not to be used by any facet because they represent older facets which
 * have been deprecated and cannot be reused for newer facets for backward compatibility reasons
 * (for example, if a dependant vajram hosted in a different runtime is still running an old build
 * where the facet using the older serial id is not yet removed or when the values of facets are
 * persisted in a data store for later use where the data is in the form of a data structure with
 * facet values indexed by the serial id.)
 *
 * <p>This is similar to the <a
 * href="https://protobuf.dev/programming-guides/proto3/#fieldreserved">"reserved fields"</a>
 * feature of protobuf
 */
@Retention(SOURCE)
public @interface ReservedSerialIds {
  int[] value();
}
