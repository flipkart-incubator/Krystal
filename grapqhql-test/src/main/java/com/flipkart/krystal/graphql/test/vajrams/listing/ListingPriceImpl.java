package com.flipkart.krystal.graphql.test.vajrams.listing;

import static com.flipkart.krystal.datatypes.StringType.string;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.graphql.test.vajrams.listing.LisitngPriceInputUtil.AllInputs;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.concurrent.CompletableFuture;

public class ListingPriceImpl extends ListingPrice {

  @Override
  public ImmutableCollection<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder().name("listing_id").type(string()).isMandatory().build());
  }

  @Override
  public ImmutableMap<Inputs, CompletableFuture<Double>> execute(ImmutableList<Inputs> inputs) {
    Builder<Inputs, CompletableFuture<Double>> builder = ImmutableMap.builder();
    for (Inputs input : inputs) {
      builder.put(input, getPrice(new AllInputs(input.getInputValueOrThrow("listing_id"))));
    }
    return builder.build();
  }
}
