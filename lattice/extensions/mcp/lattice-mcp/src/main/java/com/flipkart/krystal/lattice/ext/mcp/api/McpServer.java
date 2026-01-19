package com.flipkart.krystal.lattice.ext.mcp.api;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.lattice.core.LatticeApplication;
import com.flipkart.krystal.vajram.VajramDefRoot;
import java.lang.annotation.Retention;

/**
 * Indicates that the {@link LatticeApplication} on which this annotation is placed acts as an <a
 * href="https://modelcontextprotocol.io/specification/2025-11-25/server">MCP Server</a>
 *
 * <p>ImplNote: MCP Server Prompts are not yet supported
 */
@Retention(RUNTIME)
public @interface McpServer {

  /**
   * Classes of vajrams which act as <a
   * href="https://modelcontextprotocol.io/specification/2025-11-25/server/tools">MCP tools</a>
   */
  Class<? extends VajramDefRoot<?>>[] toolVajrams() default {};

  /**
   * Classes of vajrams which act as <a
   * href="https://modelcontextprotocol.io/specification/2025-11-25/server/resources">MCP
   * Resources</a>
   */
  Class<? extends VajramDefRoot<?>>[] resourceVajrams() default {};

  /**
   * Classes of vajrams which act as <a
   * href="https://modelcontextprotocol.io/specification/2025-11-25/server/resources#resource-templates">MCP
   * Resource Templates</a>
   */
  Class<? extends VajramDefRoot<?>>[] resourceTemplateVajrams() default {};
}
