package com.flipkart.krystal.vajram.inputs;

import org.checkerframework.checker.nullness.qual.Nullable;

public record QualifiedInputId(String dependencyName, @Nullable String vajramId, String inputName) {

}
