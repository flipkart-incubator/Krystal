package com.flipkart.krystal.krystex;

import com.flipkart.krystal.krystex.resolution.MultiResolver;
import com.flipkart.krystal.krystex.resolution.ResolverLogic;

public sealed interface Logic permits MainLogic, ResolverLogic, MultiResolver {}
