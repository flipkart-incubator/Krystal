package com.flipkart.krystal.vajram;


import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.datatypes.ObjectType;
import com.flipkart.krystal.vajram.das.DataAccessSpec;
import java.util.Collection;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;

@ToString
public final class VajramID implements DataAccessSpec {

  private final String vajramId;
  private final DataType<?> responseType;

  private VajramID(String vajramId, DataType<?> responseType) {
    this.vajramId = vajramId;
    this.responseType = responseType;
  }

  public static VajramID vajramID(String id) {
    return new VajramID(id, ObjectType.object());
  }

  public static VajramID vajramID(String id, DataType<?> responseType) {
    return new VajramID(id, responseType);
  }

  public String vajramId() {
    return vajramId;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof VajramID)) {
      return false;
    }
    return this.vajramId().equals(((VajramID) obj).vajramId());
  }

  @Override
  public int hashCode() {
    return vajramId().hashCode();
  }

  @Override
  public <T> T adapt(Collection<T> dataObjects) {
    throw new UnsupportedOperationException("");
  }

  public DataType<?> responseType() {
    return responseType;
  }
}
