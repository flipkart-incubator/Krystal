package com.flipkart.krystal.vajram;

abstract sealed class AbstractVajram<T> implements Vajram<T> permits ComputeVajram, IOVajram {}
