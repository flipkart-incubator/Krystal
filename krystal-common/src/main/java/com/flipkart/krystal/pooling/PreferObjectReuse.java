package com.flipkart.krystal.pooling;

import java.util.Optional;

public record PreferObjectReuse(int maxActiveLeasesPerObject, Optional<Integer> maxActiveObjects)
    implements MultiLeasePolicy {}
