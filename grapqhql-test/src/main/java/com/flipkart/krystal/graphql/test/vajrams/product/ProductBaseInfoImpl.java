package com.flipkart.krystal.graphql.test.vajrams.product;

import static com.flipkart.krystal.datatypes.StringType.string;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.graphql.test.models.Product;
import com.flipkart.krystal.graphql.test.vajrams.product.ProductBaseInfoInputUtil.AllInputs;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.CompletableFuture;

public class ProductBaseInfoImpl extends ProductBaseInfo {

  @Override
  public ImmutableCollection<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder().name("product_id").type(string()).isMandatory().build());
  }

  @Override
  public ImmutableMap<Inputs, CompletableFuture<Product>> execute(ImmutableList<Inputs> inputs) {
    ImmutableMap.Builder<Inputs, CompletableFuture<Product>> result = ImmutableMap.builder();
    for (Inputs input : inputs) {
      result.put(
          input, getProductBaseInfo(new AllInputs(input.getInputValueOrThrow("product_id"))));
    }
    return result.build();
  }
}
