package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.das.DataAccessSpec;
import com.flipkart.krystal.vajram.das.GraphQl;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * An index of vajrams which supports their retrieval by an access spec in sublinear time complexity
 * wrt. to the number of vajrams in the index.
 */
final class VajramIndex {
  private static final Set<Class<? extends DataAccessSpec>> SUPPORTED_ACCESS_SPECS =
      Set.of(VajramID.class, GraphQl.class);
  private final Map<Class<? extends DataAccessSpec>, AccessSpecIndex<? extends DataAccessSpec>>
      accessSpecIndices = new HashMap<>();

  public VajramIndex() {
    SUPPORTED_ACCESS_SPECS.forEach(
        aClass -> {
          if (VajramID.class.equals(aClass)) {
            accessSpecIndices.put(VajramID.class, new VajramIDIndex());
          } else if (GraphQl.class.equals(aClass)) {
            accessSpecIndices.put(GraphQl.class, new GraphQlIndex());
          }
        });
  }

  public <T extends DataAccessSpec> AccessSpecMatchingResult<T> getVajrams(T accessSpec) {
    //noinspection unchecked
    return ((AccessSpecIndex<T>) accessSpecIndices.get(accessSpec.getClass()))
        .getVajrams(accessSpec);
  }

  public void add(Vajram<?> vajram) {
    accessSpecIndices.values().forEach(accessSpecIndex -> accessSpecIndex.add(vajram));
  }
}
