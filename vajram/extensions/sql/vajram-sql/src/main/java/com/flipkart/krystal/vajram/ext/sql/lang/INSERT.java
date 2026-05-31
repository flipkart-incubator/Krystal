package com.flipkart.krystal.vajram.ext.sql.lang;

import static java.lang.annotation.ElementType.TYPE;

import com.flipkart.krystal.core.KrystalElement.Trait;
import java.lang.annotation.Target;

/**
 * Annotation to mark a @{@link Trait} interface as an SQL INSERT statement. The trait's {@code
 * _Inputs} should contain one or more inputs whose types are annotated with {@code @Table}. Each
 * input represents a row (or rows, if the type is {@code List<@Table>}) to insert into the table.
 *
 * <p>The trait must return {@code int} — the number of rows inserted.
 *
 * <p>If there are multiple inputs, they must all reference the <b>same</b> {@code @Table} type.
 */
@Target(TYPE)
public @interface INSERT {}
