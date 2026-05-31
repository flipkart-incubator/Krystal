package com.flipkart.krystal.krystex.testfixtures;

import com.flipkart.krystal.data.ErrableFacetValue;
import com.flipkart.krystal.data.Request;
import java.util.Map;

public interface SimpleRequest<T> extends Request<T> {
  Map<String, ErrableFacetValue<Object>> _asMap();

  @Override
  SimpleRequestBuilder<T> _asBuilder();
}
