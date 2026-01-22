package com.flipkart.krystal.lattice.samples.mcp.quarkus.logic;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.lattice.samples.mcp.quarkus.models.MultiplicationTable;
import com.flipkart.krystal.lattice.samples.mcp.quarkus.models.MultiplicationTable_ImmutJson;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;

/** Returns the multiplication table up to 10 for a given input 'number' */
@InvocableOutsideGraph
@Vajram
public abstract class GetMultiplicationTable extends ComputeVajramDef<MultiplicationTable> {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    /** The number to get the multiplication table for */
    @IfAbsent(FAIL)
    int number;
  }

  @Output
  static MultiplicationTable getValue(int number) {
    return MultiplicationTable_ImmutJson._builder()
        .times1(number)
        .times2(number * 2)
        .times3(number * 3)
        .times4(number * 4)
        .times5(number * 5)
        .times6(number * 6)
        .times7(number * 7)
        .times8(number * 8)
        .times9(number * 9)
        .times10(number * 10)
        ._build();
  }
}
