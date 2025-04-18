package com.flipkart.krystal.lattice.guice;

import com.flipkart.krystal.lattice.core.Dopant;
import com.google.inject.Module;
import java.util.List;
import lombok.Builder;

public record GuiceOverrideDopant(List<Module> modules) implements Dopant {}
