package com.flipkart.krystal.lattice.ext.rest.dropwizard;

import io.dropwizard.core.setup.Environment;

/**
 * A plugin interface where application owners can implement this to enrich the Dropwizard
 * environment with their own custom servlets, filters, etc.
 */
public interface EnvironmentEnricher {
  void enrichEnvironment(Environment environment);
}
