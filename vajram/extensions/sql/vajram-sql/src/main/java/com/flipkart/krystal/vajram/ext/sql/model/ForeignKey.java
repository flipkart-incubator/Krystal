package com.flipkart.krystal.vajram.ext.sql.model;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Target;

/**
 * Marks a method in a {@link Table} model as a foreign-key column pointing to another table. The
 * method's return type <b>must match</b> the {@code @PrimaryKey} return type of the referenced
 * table. The {@link #toTable()} attribute specifies which table this FK references.
 *
 * <p><b>Type invariant (enforced at compile time):</b> the FK method's return type must be the same
 * as the {@code @PrimaryKey} method's return type in the target table. For example, if {@code
 * User.id()} returns {@code long}, then {@code Order.userId()} must also return {@code long}.
 *
 * <p>If the Java method name differs from the DB column name, use {@link Column @Column("...")}
 * alongside this annotation.
 *
 * <p><b>Bidirectional-FK invariant:</b> every {@code @ForeignKey} must be paired with a
 * corresponding {@link IncomingForeignKey} on the referenced (parent) table. The pair is required
 * whenever the relationship is used in a {@code @Selection} join (i.e., a {@code List<Selection>}
 * method on a {@code @Selection} interface). The code generator enforces this at compile time.
 *
 * <p>Example:
 *
 * <pre>{@code
 * // Child table — holds the FK column
 * @Table(name = "orders")
 * public interface Order extends TableModel {
 *   @ForeignKey(toTable = User.class)
 *   long userId();           // FK column: orders.userId → users.id (both long)
 * }
 *
 * // Parent table — reverse side (not a real DB column)
 * @Table(name = "users")
 * public interface User extends TableModel {
 *   @PrimaryKey
 *   long id();
 *
 *   @IncomingForeignKey(fromTable = Order.class)
 *   List<String> orders();  // models the one-to-many relationship
 * }
 * }</pre>
 */
@Target(METHOD)
public @interface ForeignKey {
  Class<? extends TableModel> toTable();

  String[] toColumns() default {};
}
