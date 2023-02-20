package com.flipkart.krystal.utils;

public sealed interface MultiLeasePolicy permits PreferObjectReuse, DistributeLeases {

}
