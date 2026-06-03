package com.flipkart.krystal.facets;

/**
 * This annotation is used to import vajram inputs from another module/project into the current
 * module/project to enable code generation for those.
 */
public @interface ImportVajramInputs {
  String[] fromPackages();
}
