package com.flipkart.krystal.vajram.ext.sql.model;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Target;

/**
 * Marks a method in a {@link Table} model as the reverse (incoming) side of a foreign-key
 * relationship declared with {@link ForeignKey} on the other table. This method is NOT a real
 * database column — it exists only to model the relationship bidirectionally.
 *
 * <p>The child table is inferred from the method's return type: {@code List<ChildTable>} for
 * one-to-many relationships, or {@code ChildTable} for one-to-one. The child table must itself be a
 * {@code @Table}-annotated model with a {@code @ForeignKey} pointing back to this table.
 *
 * <p><b>Bidirectional-FK invariant:</b> every {@code @ForeignKey} on a child table must be paired
 * with a corresponding {@code @IncomingForeignKey} on the parent table, and vice versa. The code
 * generator enforces this at compile time: a {@code List<@Selection>} join in a {@code @Selection}
 * interface is rejected unless both annotations are present.
 *
 * <p>Benefits of modelling both directions:
 *
 * <ul>
 *   <li>Enables one-to-many and many-to-many JOIN queries without duplicating the FK column in the
 *       parent schema.
 *   <li>Makes the retrieved data shape developer-friendly (e.g. {@code user.orders()} is navigable
 *       in Java).
 *   <li>Introduces redundancy that the code generator can validate for consistency between both
 *       tables.
 * </ul>
 *
 * <p>Example — parent table holding the incoming side:
 *
 * <pre>{@code
 * @Table(name = "users")
 * public interface User extends TableModel {
 *   @IncomingForeignKey
 *   List<Order> orders();   // reverse of Order.userId() → users.id; child inferred from List<Order>
 * }
 * }</pre>
 */
@Target(METHOD)
public @interface IncomingForeignKey {}
