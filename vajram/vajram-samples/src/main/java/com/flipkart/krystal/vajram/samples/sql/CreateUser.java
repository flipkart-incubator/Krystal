package com.flipkart.krystal.vajram.samples.sql;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.facets.One2OneCommand.executeWith;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.samples.sql.CreateUser_Fac.createUserResult_n;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.One2OneCommand;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajram.samples.sql.traits.CreateUserTrait;
import com.flipkart.krystal.vajram.samples.sql.traits.CreateUserTrait_Req;
import java.util.List;

@InvocableOutsideGraph
@Vajram
public abstract class CreateUser extends ComputeVajramDef<Long> {

  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    @IfAbsent(FAIL)
    String name;

    @IfAbsent(FAIL)
    String emailId;
  }

  static class _InternalFacets {
    @IfAbsent(FAIL)
    @Dependency(onVajram = CreateUserTrait.class)
    Long createUserResult;
  }

  //    @Override
  //    public ImmutableCollection<? extends SimpleInputResolver> getSimpleInputResolvers() {
  //      return resolve(
  //          dep(
  //              createUserResult_s,
  //              depInput(CreateUserTrait_Req.parameters_s)
  //                  .usingValueAsResolver(List::of)));
  //    }

  @Resolve(dep = createUserResult_n, depInputs = CreateUserTrait_Req.parameters_n)
  public static One2OneCommand<List<Object>> createUserCommand(String name, String emailId) {

    return executeWith(List.of(name, emailId));
  }

  @Output
  public static Long output(long createUserResult) {
    return createUserResult;
  }
}
