package com.flipkart.krystal.model;

/**
 * This annotation is used to import shared models from another module/project into the current
 * module/project to enable code generation for those models.
 */
public @interface ImportSharedModels {
  /**
   * The models to import. These models must be annotated with @ModelRoot and have isShared() set to
   * true.
   */
  Class<? extends Model>[] value() default {};

  String[] fromPackages() default {};
}
