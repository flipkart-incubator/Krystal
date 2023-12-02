package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.model.DependantChain;
import com.flipkart.krystal.model.KryonId;

public sealed interface KryonCommand permits BatchCommand, GranularCommand, Flush {
  KryonId kryonId();

  DependantChain dependantChain();
}
