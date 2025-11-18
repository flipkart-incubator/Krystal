package com.flipkart.krystal.lattice.rest.visualization;

import static com.flipkart.krystal.visualization.StaticCallGraphGenerator.generateStaticCallGraphContent;

import com.flipkart.krystal.lattice.vajram.VajramDopant;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Path("_krystal/lattice/restService")
public class StaticKrystalGraphResource {

  private final VajramDopant vajramDopant;

  public StaticKrystalGraphResource(VajramDopant vajramDopant) {
    this.vajramDopant = vajramDopant;
  }

  @Path("staticGraph.html")
  @GET
  @Produces("text/html")
  public Response getStaticGraph() {
    return Response.ok(generateStaticCallGraphContent(vajramDopant.graph(), null).html()).build();
  }
}
