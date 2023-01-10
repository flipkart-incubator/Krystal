package com.flipkart.krystal.graphql.test.vajrams.product;

import static com.flipkart.krystal.datatypes.StringType.string;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.graphql.test.models.Listing;
import com.flipkart.krystal.graphql.test.vajrams.product.PreferredListingInputUtil.AllInputs;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public class PreferredListingImpl extends PreferredListing {

  @Override
  public ImmutableCollection<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder().name("product_id").type(string()).isMandatory().build(),
        Input.builder().name("preferred_listing_id").type(string()).build());
  }

  @Override
  public ImmutableMap<Inputs, Listing> executeCompute(ImmutableList<Inputs> inputsList) {
    Builder<Inputs, Listing> builder = ImmutableMap.builder();
    for (Inputs inputs : inputsList) {
      builder.put(
          inputs,
          getPreferredListing(
              new AllInputs(
                  inputs.getInputValueOrThrow("product_id"),
                  inputs.getInputValueOrDefault("preferred_listing_id", null))));
    }
    return builder.build();
  }
}
