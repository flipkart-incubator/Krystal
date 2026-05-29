package com.flipkart.krystal.lattice.ext.rest.jakarta;

import jakarta.servlet.ServletContext;

public interface ServletContextEnricher {
  void enrichServletContext(ServletContext servletContext);
}
