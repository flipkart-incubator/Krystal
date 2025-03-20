package com.flipkart.krystal.facets;

import static com.flipkart.krystal.tags.ElementTags.emptyTags;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.tags.ElementTags;

public interface BasicFacetInfo {
  int id();

  String name();

  String documentation();

  VajramID ofVajramID();

  default ElementTags tags() {
    return emptyTags();
  }
}
