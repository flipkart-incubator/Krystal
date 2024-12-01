package com.flipkart.krystal.tags;

import static com.flipkart.krystal.tags.ElementTags.emptyTags;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ElementTagsTest {

  @Test
  void of_emptyList_returnEmptySingleton() {
    assertEquals(emptyTags(), ElementTags.of(List.of()));
  }

  @Test
  void of_emptyArray_returnsEmptySingleton() {
    assertEquals(emptyTags(), ElementTags.of());
  }
}
