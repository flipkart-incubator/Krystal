package com.flipkart.krystal.vajram.samples.chess;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;

@Vajram
abstract class GetKnight extends ComputeVajramDef<Knight> implements GetPiece<Knight> {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    @IfAbsent(FAIL)
    PieceType type;
  }

  @Output
  static Knight get() {
    return new Knight();
  }
}
