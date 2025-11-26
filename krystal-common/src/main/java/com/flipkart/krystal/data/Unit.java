package com.flipkart.krystal.data;

/**
 * A singleton class representing an "absent" value or no value. This is similar to the {@link Void}
 * class of JDK, with the only difference being that a variable of this type can be assigned the
 * singleton value. This allows Krystal to represent an "absent" value without having to use a null
 * value (which is inevitable if we use Void). This is needed because validations like mandatory
 * facet validation are designed to fail when a facet value is null - so if a vajram whose output
 * type is Void returns a null value when the vajram is successful, a client vajram might still fail
 * when it uses the @IfAbsent(FAIL) validation. Using Unit instead of Void is a way to avoid this.
 *
 * <p>The name of this class is inspired from the <a
 * href="https://www.scala-lang.org/api/current/scala/Unit.html">Unit type in the Scala
 * language</a>.
 */
public final class Unit {
  @SuppressWarnings("InstantiationOfUtilityClass")
  private static final Unit UNIT = new Unit();

  public static Unit instance() {
    return UNIT;
  }

  private Unit() {}
}
