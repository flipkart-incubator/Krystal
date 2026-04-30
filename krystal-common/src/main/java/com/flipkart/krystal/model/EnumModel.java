package com.flipkart.krystal.model;

/**
 * Marker interface for Krystal model enums. Java enums annotated with {@link ModelRoot} should
 * implement this interface.
 *
 * <p>Enum models must satisfy the following constraints:
 *
 * <ul>
 *   <li>The first enum constant must always be {@code UNKNOWN}.
 *   <li>If {@link com.flipkart.krystal.serial.SerialId @SerialId} is used on any constant, {@code
 *       UNKNOWN} must have {@code @SerialId(0)}.
 * </ul>
 */
public interface EnumModel extends ImmutableModel {
  default EnumModel.Builder _asBuilder() {
    return new Builder(this);
  }

  @Override
  default EnumModel _newCopy() {
    return this;
  }

  /**
   * A dummy builder with no functionality (since enums are immutable by design). This class exists
   * only to conform to the Model interface's requirement to support _asBuilder
   */
  final class Builder implements ImmutableModel.Builder {

    private final EnumModel enumModel;

    private Builder(EnumModel enumModel) {
      this.enumModel = enumModel;
    }

    @Override
    public EnumModel _build() {
      return enumModel;
    }

    @Override
    public Builder _newCopy() {
      return this;
    }
  }
}
