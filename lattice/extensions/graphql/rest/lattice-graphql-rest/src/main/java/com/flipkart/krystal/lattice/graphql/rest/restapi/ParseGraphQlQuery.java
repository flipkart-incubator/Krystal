package com.flipkart.krystal.lattice.graphql.rest.restapi;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import graphql.language.Document;
import java.util.concurrent.CompletableFuture;

@Vajram
abstract class ParseGraphQlQuery extends IOVajramDef<Document> {
  static class _Inputs {
    @IfAbsent(FAIL)
    String query;
  }

  @Output
  static CompletableFuture<Document> output() {
    // TODO
    throw new UnsupportedOperationException();
  }
}
