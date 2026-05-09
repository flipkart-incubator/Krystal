package com.flipkart.krystal.vajram.ext.sql.model;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Target;

/**
 * Marks a method in a {@link Table} model as the reverse (incoming) side of a foreign-key
 * relationship declared with {@link ForeignKey} on the other table. This method is NOT a real
 * database column — it exists only to model the relationship bidirectionally.
 *
 * <p>The method's return type should be {@code List<ChildTable>} (one-to-many) or {@code
 * ChildTable} (one-to-one), where {@code ChildTable} is the table that holds the {@link
 * ForeignKey}.
 *
 * <p><b>Bidirectional-FK invariant:</b> every {@code @ForeignKey} on a child table must be paired
 * with a corresponding {@code @IncomingForeignKey} on the parent table, and vice versa. The code
 * generator enforces this at compile time: a {@code List<@Projection>} join in a
 * {@code @Projection} interface is rejected unless both annotations are present.
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
 *   List<Order> orders();   // reverse of Order.userId() → users.id
 * }
 * }</pre>
 */
@Target(METHOD)
public @interface IncomingForeignKey {}
