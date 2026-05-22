package com.flipkart.krystal.lattice.core.headers;

import com.google.common.collect.ImmutableList;

record HeaderImpl(String name, ImmutableList<String> values) implements Header {}
