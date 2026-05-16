package com.flipkart.krystal.annos;

import com.flipkart.krystal.core.ElementTagUtils;
import com.flipkart.krystal.data.MutatesState;
import com.google.common.collect.Comparators;
import java.lang.annotation.Annotation;
import java.util.Collection;

@ElementTagUtilityOf(CallGraphDelegationMode.class)
public class CallGraphDelegationModes implements ElementTagUtils<CallGraphDelegationMode> {

  @Override
  public CallGraphDelegationMode resolve(Collection<Annotation> annotations) {
    CallGraphDelegationMode max = null;
    for (Annotation annotation : annotations) {
      CallGraphDelegationMode current = asCallGraphDelegationMode(annotation);
      if (max == null) {
        max = current;
        continue;
      }
      max = Comparators.max(max, current, this::compare);
    }
    return max;
  }

  @Override
  public int compare(Annotation a1, Annotation a2) {
    return Integer.compare(
        precedence(asCallGraphDelegationMode(a1)), precedence(asCallGraphDelegationMode(a2)));
  }

  private int precedence(CallGraphDelegationMode callGraphDelegationMode) {
    return switch (callGraphDelegationMode.value()) {
      //    case ASYNC -> 2; //When support is added
      case SYNC -> 1;
      case NONE -> 0;
    };
  }

  private static CallGraphDelegationMode asCallGraphDelegationMode(Annotation annotation) {
    if (!(annotation instanceof CallGraphDelegationMode callGraphDelegationMode)) {
      throw new IllegalArgumentException(
          "CallGraphDelegationModes only supports handling @CallGraphDelegationMode. Found: "
              + annotation);
    }
    return callGraphDelegationMode;
  }
}
