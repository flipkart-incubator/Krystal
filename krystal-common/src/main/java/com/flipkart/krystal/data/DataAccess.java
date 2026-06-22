package com.flipkart.krystal.data;

import static com.flipkart.krystal.data.DataAccess.AccessPattern.QUERY;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.annos.ApplicableToElements;
import com.flipkart.krystal.annos.ElementTagUtility;
import com.flipkart.krystal.annos.Transitive;
import com.flipkart.krystal.core.KrystalElement.OutputLogic;
import com.flipkart.krystal.core.KrystalElement.Vajram;
import com.flipkart.krystal.data.DataAccess.DataAccesses;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that a vajram directly or indirectly (via its dependencies) handles some entity's
 * state. The state of an entity includes both in-memory state local to the current process or
 * external state such as databases or distributed caches.
 *
 * <p>This annotation represents a transitive property of a vajram. i.e. this property of a vajram
 * can impact the value of its client vajrams. Refer {@link DataAccessUtil} for how conflicts are
 * resolved when a vajram is induced with multiple transitive annotations
 */
@Target({TYPE, METHOD})
@Retention(RUNTIME)
@Repeatable(DataAccesses.class)
@ApplicableToElements(Vajram.class)
@ElementTagUtility(DataAccessUtil.class)
@Transitive
public @interface DataAccess {

  /**
   * The dataset's fully qualified names whose data this vajram access. The name must be a qualified
   * Java name (javax.lang.model.SourceVersion#isName(CharSequence) must return true)
   *
   * <ul>
   *   <li>If {@link #accessPattern()} is MUTATION, this denotes the dataset whose state this vajram
   *       mutates.
   *   <li>If {@link #accessPattern()} is QUERY, this denotes the dataset whose state this vajram
   *       reads.
   * </ul>
   *
   * If the name {@link String#isBlank() isBlank}, it means any/all datasets.
   */
  String datasetName() default "";

  AccessPattern accessPattern() default QUERY;

  enum AccessPattern {
    QUERY,
    MUTATION
  }

  @Target(TYPE)
  @Retention(RUNTIME)
  @interface DataAccesses {
    DataAccess[] value() default {};
  }
}
