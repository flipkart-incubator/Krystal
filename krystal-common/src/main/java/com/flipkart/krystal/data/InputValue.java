package com.flipkart.krystal.data;

public sealed interface InputValue<T> permits ValueOrError, Results {}
