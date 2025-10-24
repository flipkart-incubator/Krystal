package com.flipkart.krystal.vajram.samples.sql;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.samples.sql.GetUserById_Fac.userId_s;
import static com.flipkart.krystal.vajram.samples.sql.GetUserById_Fac.user_s;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajram.samples.sql.traits.GetUserByIdTrait;
import com.flipkart.krystal.vajram.samples.sql.traits.GetUserByIdTrait_Req;
import com.google.common.collect.ImmutableCollection;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

@InvocableOutsideGraph
@Vajram
public abstract class GetUserById extends ComputeVajramDef<User> {

  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    @IfAbsent(FAIL)
    Integer userId;
  }

  static class _InternalFacets {
    @Dependency(onVajram = GetUserByIdTrait.class)
    User user;
  }

  @Override
  public ImmutableCollection<? extends SimpleInputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(
            user_s,
            depInput(GetUserByIdTrait_Req.parameters_s).using(userId_s).asResolver(List::of)));
  }

  @Output
  public static User output(@Nullable User user) {
    return user;
  }
}
