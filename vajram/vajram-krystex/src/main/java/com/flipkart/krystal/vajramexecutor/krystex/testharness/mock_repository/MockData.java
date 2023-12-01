package com.flipkart.krystal.vajramexecutor.krystex.testharness.mock_repository;

import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.vajram.VajramRequest;

public record MockData<T>(VajramRequest<T> request, ValueOrError<T> response){}
