package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelClusterRoot;
import graphql.GraphQLError;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

@ModelClusterRoot(
    immutableRoot = GraphQlTypeModel_Immut.class,
    builderRoot = GraphQlTypeModel_Immut.Builder.class)
public interface GraphQlTypeModel extends Model {
  /**
   * Returns the __typename of a graphql type according to the graphql spec or null if it has yet
   * been computed/set.
   */
  @Nullable String __typename();
}
