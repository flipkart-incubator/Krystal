package com.flipkart.krystal.vajram.samples.sql;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.samples.sql.GetAllUsers_Fac.allUsers_s;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajram.samples.sql.traits.GetAllUsersTrait;
import com.flipkart.krystal.vajram.samples.sql.traits.GetAllUsersTrait_Req;
import com.google.common.collect.ImmutableCollection;
import java.util.List;

@InvocableOutsideGraph
@Vajram
public abstract class GetAllUsers extends ComputeVajramDef<List<User>> {

  static class _InternalFacets {
    @IfAbsent(FAIL)
    @Dependency(onVajram = GetAllUsersTrait.class)
    List<User> allUsers;
  }

  @Override
  public ImmutableCollection<? extends SimpleInputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(
            allUsers_s,
            depInput(GetAllUsersTrait_Req.parameters_s).usingValueAsResolver(List::of)));
  }

  @Output
  public static List<User> output(List<User> allUsers) {
    return allUsers;
  }
}
