package com.flipkart.krystal.krystex.resolution;

import com.flipkart.krystal.data.RequestBuilder;
import com.flipkart.krystal.krystex.Logic;
import com.flipkart.krystal.krystex.OutputLogic;

@FunctionalInterface
public non-sealed interface CreateNewRequest extends Logic {
  RequestBuilder<Object> newRequestBuilder();
}
