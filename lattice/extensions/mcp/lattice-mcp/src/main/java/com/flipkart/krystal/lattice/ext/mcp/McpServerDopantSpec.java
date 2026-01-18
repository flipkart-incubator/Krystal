package com.flipkart.krystal.lattice.ext.mcp;

import static com.flipkart.krystal.lattice.ext.mcp.McpServerDopant.MCP_SERVER_DOPANT_TYPE;

import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoConfiguration;
import com.flipkart.krystal.lattice.core.doping.DopantSpec;
import com.flipkart.krystal.lattice.core.doping.DopantSpecBuilder;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.ext.mcp.api.McpServer;
import lombok.Builder;

@DopantType(MCP_SERVER_DOPANT_TYPE)
public record McpServerDopantSpec()
    implements DopantSpec<McpServer, NoConfiguration, McpServerDopant> {

  @Builder(buildMethodName = "_buildSpec")
  public McpServerDopantSpec {}

  @Override
  public Class<? extends McpServerDopant> dopantClass() {
    return McpServerDopant.class;
  }

  @Override
  public String _dopantType() {
    return MCP_SERVER_DOPANT_TYPE;
  }

  @Override
  public Class<NoConfiguration> _configurationType() {
    return NoConfiguration.class;
  }

  public static final class McpServerDopantSpecBuilder
      implements DopantSpecBuilder<McpServer, NoConfiguration, McpServerDopantSpec> {

    @Override
    public Class<McpServer> _annotationType() {
      return McpServer.class;
    }
  }
}
