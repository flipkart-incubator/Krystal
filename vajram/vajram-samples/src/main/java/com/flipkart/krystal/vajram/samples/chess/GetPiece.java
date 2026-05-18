package com.flipkart.krystal.vajram.samples.chess;

import static com.flipkart.krystal.annos.ComputeDelegationMode.SYNC;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.annos.CallGraphDelegationMode;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.traits.UseForPredicateDispatch;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.TraitDef;

@Trait
@CallGraphDelegationMode(SYNC)
public interface GetPiece<T extends Piece> extends TraitDef<T> {
  @SuppressWarnings("initialization.field.uninitialized")
  class _Inputs {
    @UseForPredicateDispatch
    @IfAbsent(FAIL)
    PieceType type;
  }
}
