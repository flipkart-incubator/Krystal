package com.flipkart.krystal.lattice.samples.mcp.quarkus.logic;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;

/** Returns the Preamble of the Constitution of India */
@InvocableOutsideGraph
@Vajram
public abstract class GetPreambleOfIndia extends ComputeVajramDef<String> {

  @Output
  static String preamble() {
    return """
        WE, THE PEOPLE OF INDIA, having solemnly resolved to constitute India into a SOVEREIGN SOCIALIST SECULAR DEMOCRATIC REPUBLIC and to secure to all its citizens:
        JUSTICE, social, economic and political;
        LIBERTY of thought, expression, belief, faith and worship;
        EQUALITY of status and of opportunity;
        and to promote among them all
        FRATERNITY assuring the dignity of the individual and the unity and integrity of the Nation;
        IN OUR CONSTITUENT ASSEMBLY this twenty-sixth day of November, 1949, do HEREBY ADOPT, ENACT AND GIVE TO OURSELVES THIS CONSTITUTION.
        """;
  }
}
