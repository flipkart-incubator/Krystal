package com.flipkart.krystal.utils;

import java.util.Optional;

public record PreferObjectReuse(int maxActiveLeasesPerObject, Optional<Integer> maxActiveObjects)
    implements MultiLeasePolicy {}
