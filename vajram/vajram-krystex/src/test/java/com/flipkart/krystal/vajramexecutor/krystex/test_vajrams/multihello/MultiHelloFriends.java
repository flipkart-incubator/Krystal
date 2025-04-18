package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello;

import static com.flipkart.krystal.data.IfNull.IfNullThen.FAIL;
import static com.flipkart.krystal.vajram.facets.FanoutCommand.executeFanoutWith;
import static com.flipkart.krystal.vajram.facets.FanoutCommand.skipFanout;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInputFanout;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriends_Fac.audited_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriends_Fac.hellos_n;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriends_Fac.hellos_s;
import static java.lang.System.lineSeparator;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.IfNull;
import com.flipkart.krystal.data.RequestResponse;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.FanoutCommand;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.audit.AuditData;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.audit.AuditData_Req;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriends;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriends_ImmutReqPojo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriends_ImmutReqPojo.Builder;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriends_Req;
import com.google.common.collect.ImmutableCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExternallyInvocable
@Vajram
public abstract class MultiHelloFriends extends ComputeVajramDef<String> {
  static class _Inputs {
    @IfNull(FAIL)
    List<String> userIds;

    boolean skip;
  }

  static class _InternalFacets {
    @Dependency(onVajram = HelloFriends.class, canFanout = true)
    String hellos;

    @Dependency(onVajram = AuditData.class, canFanout = true)
    Void audited;
  }

  private static final List<Integer> NUMBER_OF_FRIENDS = List.of(1, 2);

  @Override
  public ImmutableCollection<? extends SimpleInputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(
            audited_s,
            depInputFanout(AuditData_Req.data_s)
                .using(hellos_s)
                .skipIf(hellos -> hellos.requestResponsePairs().isEmpty(), "Nothing to audit")
                .asResolver(
                    hellos ->
                        hellos.requestResponsePairs().stream()
                            .map(RequestResponse::response)
                            .map(Errable::valueOpt)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .toList())));
  }

  @Resolve(
      dep = hellos_n,
      depInputs = {HelloFriends_Req.userId_n, HelloFriends_Req.numberOfFriends_n})
  static FanoutCommand<Builder> sayHello(List<String> userIds, Optional<Boolean> skip) {
    if (skip.orElse(false)) {
      return skipFanout("skip requested");
    }
    List<HelloFriends_ImmutReqPojo.Builder> requests = new ArrayList<>();
    for (String userId : userIds) {
      for (int numberOfFriend : NUMBER_OF_FRIENDS) {
        requests.add(
            HelloFriends_ImmutReqPojo._builder().userId(userId).numberOfFriends(numberOfFriend));
      }
    }
    return executeFanoutWith(requests);
  }

  @Output
  static String sayHellos(
      List<String> userIds,
      FanoutDepResponses<String, HelloFriends_Req> hellos,
      FanoutDepResponses<Void, AuditData_Req> audited) {
    List<String> result = new ArrayList<>();
    for (String userId : userIds) {
      for (Integer numberOfFriend : NUMBER_OF_FRIENDS) {
        hellos
            .getForRequest(
                HelloFriends_ImmutReqPojo._builder()
                    .userId(userId)
                    .numberOfFriends(numberOfFriend)
                    ._build())
            .valueOpt()
            .ifPresent(result::add);
      }
    }
    log.debug(
        audited.requestResponsePairs().stream()
            .map(RequestResponse::request)
            .map(Object::toString)
            .collect(Collectors.joining(lineSeparator())));
    return String.join("\n", result);
  }
}
