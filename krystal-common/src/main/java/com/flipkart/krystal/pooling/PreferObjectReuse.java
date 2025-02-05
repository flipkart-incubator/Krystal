package com.flipkart.krystal.pooling;

import java.util.Optional;

@SuppressWarnings({"optional.parameter", "optional.field"})
public record PreferObjectReuse(int maxActiveLeasesPerObject, Optional<Integer> maxActiveObjects)
    implements MultiLeasePolicy {}
