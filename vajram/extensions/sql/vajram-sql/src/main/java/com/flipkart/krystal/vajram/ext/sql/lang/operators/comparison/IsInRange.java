package com.flipkart.krystal.vajram.ext.sql.lang.operators.comparison;

import static java.lang.annotation.ElementType.METHOD;

import com.google.common.collect.Range;
import java.lang.annotation.Target;

/**
 * Range comparison operator where a column value is verified to lie within the lower bound and
 * upper bound of a {@link Range}. This supports closed, open, and half-open/half-closed ranges.
 *
 * <p>The annotated method must return {@code Range<T>} where {@code T} is a comparable type:
 * numeric primitives/boxed ({@code int}, {@code long}, …), or temporal types ({@code LocalDate},
 * {@code LocalDateTime}, {@code OffsetDateTime}).
 *
 * <p>At runtime, the {@link Range} object determines which SQL operators are used:
 *
 * <ul>
 *   <li>{@code Range.closed(a, b)} → {@code column >= $1 AND column <= $2}
 *   <li>{@code Range.open(a, b)} → {@code column > $1 AND column < $2}
 *   <li>{@code Range.closedOpen(a, b)} → {@code column >= $1 AND column < $2}
 *   <li>{@code Range.openClosed(a, b)} → {@code column > $1 AND column <= $2}
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @Column("orderTime")
 * @IsInRange
 * Range<Long> orderTimeRange();
 * }</pre>
 */
@Target(METHOD)
public @interface IsInRange {}
