package com.flipkart.krystal.data;

import com.flipkart.krystal.annos.ElementTagUtilityOf;
import com.flipkart.krystal.core.ElementTagUtils;
import com.google.common.collect.Comparators;
import java.lang.annotation.Annotation;
import java.util.Collection;

@ElementTagUtilityOf(MutatesState.class)
public class MutatesStates implements ElementTagUtils<MutatesState> {
  @Override
  public final MutatesState resolve(Collection<Annotation> annotations) {
    MutatesState max = null;
    for (Annotation annotation : annotations) {
      MutatesState current = asMutatesState(annotation);
      if (max == null) {
        max = current;
        continue;
      }
      max = Comparators.max(max, current, this::compare);
    }
    return max;
  }

  @Override
  public int compare(Annotation ms1, Annotation ms2) {
    return Integer.compare(precedence(asMutatesState(ms1)), precedence(asMutatesState(ms2)));
  }

  private static short precedence(MutatesState mutatesState) {
    return switch (mutatesState.value()) {
      case TRUE -> 2;
      case UNKNOWN -> 1;
      case FALSE -> 0;
    };
  }

  private static MutatesState asMutatesState(Annotation annotation) {
    if (!(annotation instanceof MutatesState mutatesState)) {
      throw new IllegalArgumentException(
          "MutatesStates only supports handling @MutatesState. Found: " + annotation);
    }
    return mutatesState;
  }
}
