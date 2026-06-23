package com.flipkart.krystal.krystex.caching;

import com.flipkart.krystal.core.VajramID;
import java.util.Collections;
import java.util.List;
import java.util.Map;

record EntityWriters(Map<String, List<VajramID>> writersByEntity) {

  EntityWriters {
    writersByEntity = Collections.unmodifiableMap(writersByEntity);
  }
}
