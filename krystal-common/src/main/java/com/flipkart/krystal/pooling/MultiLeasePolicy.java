package com.flipkart.krystal.pooling;

public sealed interface MultiLeasePolicy permits PreferObjectReuse, DistributeLeases {}
