package com.flipkart.krystal.vajram.ext.sql.model;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Target;

/**
 * Annotation to mark a field in a @{@link Table} model as a foreign key from another Table. The
 * field type must be same as the model type of the otherTable. Unlike other fields in the model,
 * this particular field is not a column in the table - it used to model the foreign key
 * relationship between the two tables bidirectionally even though in the Database, it is a
 * unidirectional relationship. This kind of "incoming relationship" modelling helps in the
 * following ways:
 *
 * <ul>
 *   <li>Allows us to model one-to-many, many-to-one and many-to-many relationships in the model
 *       which is not possible just with @{@link ForeignKey} in one table. This also helps in
 *       modelling the retrieved data in a developer friendly way.
 *   <li>Allows devs to opt in to being able to query the incoming table via the current table using
 *       a JOIN.
 *   <li>Introduces redundancy in the schema allowing for validations to make sure both directions
 *       are consistent with each other.
 * </ul>
 */
@Target(METHOD)
public @interface IncomingForeignKey {}
