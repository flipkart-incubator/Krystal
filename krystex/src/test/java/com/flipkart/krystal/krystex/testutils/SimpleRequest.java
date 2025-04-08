package com.flipkart.krystal.krystex.testutils;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Request;
import java.util.Map;

public interface SimpleRequest<T> extends Request<T> {
  Map<Integer, Errable<Object>> _asMap();
}
