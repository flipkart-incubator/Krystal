package com.flipkart.krystal.vajram.samples.chess;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;

@Vajram
abstract class GetRook extends ComputeVajramDef<Rook> implements GetPiece<Rook> {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    @IfAbsent(FAIL)
    PieceType type;
  }

  @Output
  static Rook get() {
    return new Rook();
  }
}
