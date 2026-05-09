package com.flipkart.krystal.vajram.ext.sql.model;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Target;

/**
 * Marks a method in a {@link Table} model as a foreign-key column pointing to another table. The
 * method's return type must be the model type of the referenced table.
 *
 * <p><b>Bidirectional-FK invariant:</b> every {@code @ForeignKey} must be paired with a
 * corresponding {@link IncomingForeignKey} on the referenced (parent) table. The pair is required
 * whenever the relationship is used in a {@code @Projection} join (i.e., a {@code List<Projection>}
 * method on a {@code @Projection} interface). The code generator enforces this at compile time.
 *
 * <p>Example:
 *
 * <pre>{@code
 * // Child table — holds the FK column
 * @Table(name = "orders")
 * public interface Order extends TableModel {
 *   @ForeignKey
 *   User userId();         // FK column: orders.userId → users.id
 * }
 *
 * // Parent table — reverse side (not a real DB column)
 * @Table(name = "users")
 * public interface User extends TableModel {
 *   @IncomingForeignKey
 *   List<Order> orders();  // models the one-to-many relationship
 * }
 * }</pre>
 */
@Target(METHOD)
public @interface ForeignKey {
  String[] toColumns() default {};
}
