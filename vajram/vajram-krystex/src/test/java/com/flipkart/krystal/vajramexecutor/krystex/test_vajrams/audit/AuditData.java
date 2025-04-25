package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.audit;

import static com.flipkart.krystal.data.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.data.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Vajram
public abstract class AuditData extends ComputeVajramDef<Void> {
  static class _Inputs {
    @IfAbsent(FAIL)
    String data;
  }

  @Output
  static Void output(String data) {
    log.debug(data);
    return null;
  }
}
