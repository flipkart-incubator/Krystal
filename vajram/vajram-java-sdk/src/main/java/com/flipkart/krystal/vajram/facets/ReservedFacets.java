package com.flipkart.krystal.vajram.facets;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * FacetIds which are not to be used by any facet because they represent older facets which have
 * been deprecated and cannot be reused for newer facets for backward compatibility reasons (for
 * example, if a dependant vajram hosted in a different runtime is still running an old build where
 * the facet using the older index is not yet removed or when the values of facets are persisted in
 * a data store for later use where the data is in the form of a data structure with fields indexed
 * by the facet id.)
 *
 * <p>This is similar to the <a
 * href="https://protobuf.dev/programming-guides/proto3/#fieldreserved">"reserved fields"</a>
 * feature of protobuf
 */
@Target(ElementType.TYPE)
public @interface ReservedFacets {
  int[] ids();
}
