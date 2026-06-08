package com.flipkart.krystal.vajram.ext.sql.vertx.codegen;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import javax.lang.model.element.TypeElement;

public record SqlTableGenContext(CodeGenUtility util, TypeElement tableElement) {}
