package com.flipkart.krystal.data;

import com.flipkart.krystal.annos.ElementTagUtilityOf;
import com.flipkart.krystal.core.ElementTagUtils;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.stream.Collectors;

/** Element Tag Utility to handle @DataAccess annotations */
@ElementTagUtilityOf(DataAccess.class)
public class DataAccessUtil implements ElementTagUtils<DataAccess> {
  @Override
  public final Collection<DataAccess> resolve(Collection<Annotation> annotations) {
    return annotations.stream().map(DataAccessUtil::asDataAccess).collect(Collectors.toSet());
  }

  @Override
  public int compare(Annotation ms1, Annotation ms2) {
    DataAccess dataAccess1 = asDataAccess(ms1);
    DataAccess dataAccess2 = asDataAccess(ms2);
    if (!dataAccess1.datasetName().strip().equals(dataAccess2.datasetName().strip())) {
      return 0;
    }
    return Integer.compare(precedence(dataAccess1), precedence(dataAccess2));
  }

  private static short precedence(DataAccess dataAccess) {
    return switch (dataAccess.accessPattern()) {
      case MUTATION -> 1;
      case QUERY -> 0;
    };
  }

  private static DataAccess asDataAccess(Annotation annotation) {
    if (!(annotation instanceof DataAccess dataAccess)) {
      throw new IllegalArgumentException(
          "DataAccessUtil only supports handling @DataAccess. Found: " + annotation);
    }
    return dataAccess;
  }
}
