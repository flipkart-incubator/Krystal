package com.flipkart.krystal.lattice.samples.mcp.quarkus.logic;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static java.util.Objects.requireNonNullElse;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import java.net.URI;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Responds a hello to the user, along with the complete resource template uri */
@InvocableOutsideGraph
@Vajram
public abstract class HelloUserWithUri extends ComputeVajramDef<String> {

  static class _Inputs {
    @IfAbsent(FAIL)
    URI uri;

    String name;
  }

  @Output
  static String echo(URI uri, @Nullable String name) {
    return "Hello "
        + requireNonNullElse(name, "User")
        + "! Received URI with uri: "
        + uri.toString();
  }
}
