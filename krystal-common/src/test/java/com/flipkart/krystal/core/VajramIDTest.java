package com.flipkart.krystal.core;

import static com.flipkart.krystal.core.VajramID.vajramID;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VajramIDTest {

  @Test
  void sameIdReturnsSameInstance() {
    VajramID first = vajramID("testVajram");
    VajramID second = vajramID("testVajram");
    assertThat(first).isSameAs(second);
  }

  @Test
  void differentIdsReturnDifferentInstances() {
    VajramID a = vajramID("vajramA");
    VajramID b = vajramID("vajramB");
    assertThat(a).isNotSameAs(b);
  }

  @Test
  void internedInstancesAreEqualByReference() {
    VajramID first = vajramID("internedVajram");
    VajramID second = vajramID("internedVajram");
    // interning guarantees reference equality, so == is equivalent to equals
    assertThat(first == second).isTrue();
  }

  @Test
  void idValueIsPreserved() {
    assertThat(vajramID("myVajram").id()).isEqualTo("myVajram");
  }

  @Test
  void toStringFormat() {
    assertThat(vajramID("foo").toString()).isEqualTo("v<foo>");
  }
}
