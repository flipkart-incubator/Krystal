package com.flipkart.krystal.vajram.das;

import com.flipkart.krystal.vajram.VajramID;
import java.util.Collection;

/**
 * A spec which can be used by the Krystal runtime to identify a Vajram which needs to be executed.
 * This is generally used by clients of a vajram to declare their dependency on that vajram.
 */
public sealed interface DataAccessSpec permits VajramID, GraphQl {

  /**
   * Returns a Data Object whose data conforms to this access spec. Only the data present in the
   * {@code dataObjects} which conforms to this spec is merged into the returned object ignoring
   * extra data.
   */
  <T> T adapt(Collection<T> dataObjects);

}
