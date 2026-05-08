package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model;

import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.vajram.ext.sql.model.IncomingForeignKey;
import com.flipkart.krystal.vajram.ext.sql.model.PrimaryKey;
import com.flipkart.krystal.vajram.ext.sql.model.Table;
import com.flipkart.krystal.vajram.ext.sql.model.TableModel;
import com.flipkart.krystal.vajram.ext.sql.model.UniqueKey;
import java.util.List;
import java.util.Optional;

/**
 * Sample table model for the {@code users} table.
 *
 * <p>Demonstrates:
 *
 * <ul>
 *   <li>{@code @PrimaryKey} on the {@code id} column
 *   <li>{@code @UniqueKey} on the {@code email} column (single-column unique constraint)
 *   <li>Nullable column using {@code Optional}
 *   <li>{@code @IncomingForeignKey} to model the reverse side of the FK from {@link Order}
 * </ul>
 */
@ModelRoot
@Table(name = "users")
public interface User extends TableModel {

  @PrimaryKey
  long id();

  String name();

  @UniqueKey(name = "uk_users_email")
  String email();

  Optional<String> phoneNumber();

  /**
   * Reverse side of the FK declared on {@link Order#userId()}. This is not a real column in the
   * {@code users} table — it models the one-to-many relationship for convenience.
   */
  @IncomingForeignKey
  List<Order> orders();
}
