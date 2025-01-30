package com.flipkart.krystal.data;

import java.util.Map;

public interface SimpleRequest<T> extends Request<T> {
  Map<Integer, Errable<Object>> _asMap();
}
