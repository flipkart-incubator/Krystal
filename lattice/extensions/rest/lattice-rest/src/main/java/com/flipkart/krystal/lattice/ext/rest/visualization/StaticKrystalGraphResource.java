package com.flipkart.krystal.lattice.ext.rest.visualization;

import static com.flipkart.krystal.visualization.StaticCallGraphGenerator.generateStaticCallGraphContent;

import com.flipkart.krystal.lattice.krystex.KrystexDopant;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Path("_krystal/lattice/restService")
public class StaticKrystalGraphResource {

  private final KrystexDopant krystexDopant;

  public StaticKrystalGraphResource(KrystexDopant krystexDopant) {
    this.krystexDopant = krystexDopant;
  }

  @Path("staticGraph.html")
  @GET
  @Produces("text/html")
  public Response getStaticGraph() {
    return Response.ok(generateStaticCallGraphContent(krystexDopant.executableGraph(), null).html())
        .build();
  }
}
